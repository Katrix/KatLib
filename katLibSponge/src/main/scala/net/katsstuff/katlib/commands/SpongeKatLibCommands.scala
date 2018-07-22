package net.katsstuff.katlib.commands

import scala.language.implicitConversions

import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.entity.living.player.{Player, User}
import org.spongepowered.api.text.{Text => SpongeText}
import org.spongepowered.api.world.{Location, World}

import cats.arrow.FunctionK
import cats.data.NonEmptyList
import cats.kernel.Monoid
import cats.syntax.all._
import cats.{FlatMap, MonadError, ~>}
import net.katsstuff.katlib.algebras.{Localized, Pagination, TextConversion}
import net.katsstuff.katlib.command.{CommandSyntax, KatLibCommands}
import net.katsstuff.katlib.helper.Implicits._
import net.katsstuff.minejson.text.Text
import net.katsstuff.scammander.sponge.components._
import net.katsstuff.scammander.{CommandFailure, ComplexChildCommand, ComplexCommand}

abstract class SpongeKatLibCommands[G[_]: FlatMap, F[_], Page: Monoid](GtoF: G ~> F)(
    implicit pagination: Pagination.Aux[G, CommandSource, Page],
    localized: Localized[G, CommandSource],
    T: TextConversion[F],
    F: MonadError[F, NonEmptyList[CommandFailure]]
) extends KatLibCommands[G, F, Page, CommandSource, Player, User](GtoF)
    with SpongeBase[F]
    with SpongeValidators[F]
    with SpongeParameter[F]
    with SpongeOrParameter[F] { //We do not mix in the sponge help command
  import cats.instances.option._

  override def testPermission(command: SpongeCommandWrapper[F], source: CommandSource): F[Boolean] =
    command.testPermission(source).pure[F]

  override def commandUsage(command: SpongeCommandWrapper[F], source: CommandSource): F[Text] =
    T.spongeToOur(command.getUsage(source))

  override def commandHelp(command: SpongeCommandWrapper[F], source: CommandSource): F[Option[Text]] =
    command.getHelp(source).toOption.traverse(T.spongeToOur)

  override def commandDescription(command: SpongeCommandWrapper[F], source: CommandSource): F[Option[Text]] =
    command.getShortDescription(source).toOption.traverse(T.spongeToOur)

  override implicit val playerValidator: UserValidator[Player] = playerSender
  override implicit val userValidator:   UserValidator[User]   = userSender

  override implicit def commandOps(
      command: ComplexCommand
  ): CommandSyntax[F, CommandSource, Unit, Option[Location[World]], Int, SpongeCommandWrapper[F]] =
    new SpongeCommandSyntax(command, FunctionK.lift(runComputation))
}
class SpongeCommandSyntax[F[_]](
    command: ComplexCommand[F, CommandSource, Unit, Option[Location[World]], Int, SpongeCommandWrapper[F]],
    runComputation: FunctionK[F, Either[NonEmptyList[CommandFailure], ?]]
)(implicit G: MonadError[F, NonEmptyList[CommandFailure]])
    extends CommandSyntax[F, CommandSource, Unit, Option[Location[World]], Int, SpongeCommandWrapper[F]] {
  override def toChild(
      aliases: Seq[String],
      permission: Option[String],
      help: CommandSource => F[Option[String]],
      description: CommandSource => F[Option[String]]
  ): ChildCommand = ComplexChildCommand(
    aliases.toSet, //TODO: Change to take a seq
    SpongeCommandWrapper(
      command,
      CommandInfo(
        permission,
        help.andThen(_.map(_.map(SpongeText.of))),
        description.andThen(_.map(_.map(SpongeText.of)))
      ),
      runComputation
    )
  )
}
