package net.katsstuff.katlib.commands

import scala.language.implicitConversions

import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

import cats.arrow.FunctionK
import cats.data.NonEmptyList
import cats.kernel.Monoid
import cats.{~>, FlatMap, MonadError}
import cats.syntax.all._
import net.katsstuff.minejson.text.Text
import net.katsstuff.scammander.{CommandFailure, ComplexChildCommand, ComplexCommand}
import net.katsstuff.scammander.bukkit.components.{BukkitBaseAll, BukkitCommandWrapper, BukkitExtra, ChildCommandExtra}
import net.katstuff.katlib.algebras.{Localized, Pagination}
import net.katstuff.katlib.command.{CommandSyntax, KatLibCommands}

abstract class BukkitKatLibCommands[G[_]: FlatMap, F[_], Page: Monoid](FtoG: G ~> F)(
    implicit pagination: Pagination.Aux[G, CommandSender, Page],
    localized: Localized[G, CommandSender]
) extends KatLibCommands[G, F, Page, CommandSender, Player, OfflinePlayer](FtoG)
    with BukkitBaseAll[F] {

  override def testPermission(command: ChildCommandExtra[F], source: CommandSender): F[Boolean] =
    command.permission.forall(source.hasPermission).pure

  override def commandUsage(command: ChildCommandExtra[F], source: CommandSender): F[Text] =
    command.command.usage(source).map(Text.apply)

  override def commandHelp(command: ChildCommandExtra[F], source: CommandSender): F[Option[Text]] =
    command.help(source).map(Text.apply(_): Text).pure

  override def commandDescription(command: ChildCommandExtra[F], source: CommandSender): F[Option[Text]] =
    command.description(source).map(Text.apply(_): Text).pure

  override implicit val userParam:       Parameter[Set[OfflinePlayer]] = offlinePlayerParam
  override implicit val playerValidator: UserValidator[Player]         = playerSender
  override implicit val userValidator:   UserValidator[OfflinePlayer]  = offlinePlayerSender

  override implicit def commandOps(
      command: ComplexCommand
  ): CommandSyntax[F, CommandSender, BukkitExtra, BukkitExtra, Boolean, ChildCommandExtra[F]] =
    new BukkitCommandSyntax[F](command, FunctionK.lift(runComputation))
}

class BukkitCommandSyntax[G[_]](
    command: ComplexCommand[G, CommandSender, BukkitExtra, BukkitExtra, Boolean, ChildCommandExtra[G]],
    runComputation: FunctionK[G, Either[NonEmptyList[CommandFailure], ?]]
)(implicit G: MonadError[G, NonEmptyList[CommandFailure]])
    extends CommandSyntax[G, CommandSender, BukkitExtra, BukkitExtra, Boolean, ChildCommandExtra[G]] {

  override def toChild(
      aliases: Set[String],
      permission: Option[String],
      help: CommandSender => Option[String],
      description: CommandSender => Option[String]
  ): ChildCommand =
    ComplexChildCommand(
      aliases,
      ChildCommandExtra(BukkitCommandWrapper(command, runComputation), permission, help, description)
    )
}
