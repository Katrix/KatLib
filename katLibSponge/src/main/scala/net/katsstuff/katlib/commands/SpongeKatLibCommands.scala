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
import cats.{~>, FlatMap, MonadError}
import net.katsstuff.katlib.algebras.TextConversion
import net.katsstuff.katlib.helper.Implicits._
import net.katsstuff.minejson.text.Text
import net.katsstuff.scammander.sponge.components._
import net.katsstuff.scammander.{CommandFailure, ComplexChildCommand, ComplexCommand}
import net.katstuff.katlib.algebras.{Localized, Pagination}
import net.katstuff.katlib.command.{CommandSyntax, KatLibCommands}

abstract class SpongeKatLibCommands[G[_]: FlatMap, F[_], Page: Monoid](FtoG: G ~> F)(
    implicit pagination: Pagination.Aux[G, CommandSource, Page],
    localized: Localized[G, CommandSource],
    T: TextConversion[F],
    F: MonadError[F, NonEmptyList[CommandFailure]]
) extends KatLibCommands[G, F, Page, CommandSource, Player, User](FtoG)
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
class SpongeCommandSyntax[G[_]](
    command: ComplexCommand[G, CommandSource, Unit, Option[Location[World]], Int, SpongeCommandWrapper[G]],
    runComputation: FunctionK[G, ({ type L[A] = Either[NonEmptyList[CommandFailure], A] })#L]
)(implicit G: MonadError[G, NonEmptyList[CommandFailure]])
    extends CommandSyntax[G, CommandSource, Unit, Option[Location[World]], Int, SpongeCommandWrapper[G]] {

  override def toChild(
      aliases: Set[String],
      permission: Option[String],
      help: CommandSource => Option[String],
      description: CommandSource => Option[String]
  ): ChildCommand =
    ComplexChildCommand(
      aliases,
      SpongeCommandWrapper(
        command,
        CommandInfo(permission, help.andThen(_.map(SpongeText.of)), description.andThen(_.map(SpongeText.of))),
        runComputation
      )
    )
}
