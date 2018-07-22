package net.katsstuff.katlib.commands

import scala.language.implicitConversions

import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

import cats.arrow.FunctionK
import cats.data.NonEmptyList
import cats.kernel.Monoid
import cats.{FlatMap, MonadError, ~>}
import cats.syntax.all._
import net.katsstuff.katlib.algebras.{Localized, Pagination}
import net.katsstuff.katlib.command.{CommandSyntax, KatLibCommands}
import net.katsstuff.minejson.text.Text
import net.katsstuff.scammander.{CommandFailure, ComplexChildCommand, ComplexCommand}
import net.katsstuff.scammander.bukkit.components.{BukkitBaseAll, BukkitCommandWrapper, BukkitExtra, ChildCommandExtra}

abstract class BukkitKatLibCommands[G[_]: FlatMap, F[_], Page: Monoid](GtoF: G ~> F)(
    implicit pagination: Pagination.Aux[G, CommandSender, Page],
    localized: Localized[G, CommandSender]
) extends KatLibCommands[G, F, Page, CommandSender, Player, OfflinePlayer](GtoF)
    with BukkitBaseAll[F] {

  override def testPermission(command: ChildCommandExtra[F], source: CommandSender): F[Boolean] =
    command.permission.forall(source.hasPermission).pure

  override def commandUsage(command: ChildCommandExtra[F], source: CommandSender): F[Text] =
    command.command.usage(source).map(Text.apply)

  override def commandHelp(command: ChildCommandExtra[F], source: CommandSender): F[Option[Text]] =
    command.help(source).map(_.map(Text.apply(_): Text))

  override def commandDescription(command: ChildCommandExtra[F], source: CommandSender): F[Option[Text]] =
    command.description(source).map(_.map(Text.apply(_): Text))

  override implicit val userParam:       Parameter[Set[OfflinePlayer]] = offlinePlayerParam
  override implicit val playerValidator: UserValidator[Player]         = playerSender
  override implicit val userValidator:   UserValidator[OfflinePlayer]  = offlinePlayerSender

  override implicit def commandOps(
      command: ComplexCommand
  ): CommandSyntax[F, CommandSender, BukkitExtra, BukkitExtra, Boolean, ChildCommandExtra[F]] =
    new BukkitCommandSyntax[F](command, FunctionK.lift(runComputation))
}

class BukkitCommandSyntax[F[_]](
    command: ComplexCommand[F, CommandSender, BukkitExtra, BukkitExtra, Boolean, ChildCommandExtra[F]],
    runComputation: FunctionK[F, Either[NonEmptyList[CommandFailure], ?]]
)(implicit G: MonadError[F, NonEmptyList[CommandFailure]])
    extends CommandSyntax[F, CommandSender, BukkitExtra, BukkitExtra, Boolean, ChildCommandExtra[F]] {
  override def toChild(
      aliases: Seq[String],
      permission: Option[String],
      help: CommandSender => F[Option[String]],
      description: CommandSender => F[Option[String]]
  ): ChildCommand = ComplexChildCommand(
    aliases.toSet, //TODO: Change to Seq
    ChildCommandExtra(BukkitCommandWrapper(command, runComputation), permission, help, description)
  )
}
