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

abstract class SpongeKatLibCommands[F[_]: FlatMap, G[_], Page: Monoid](FtoG: F ~> G)(
    implicit pagination: Pagination.Aux[F, CommandSource, Page],
    localized: Localized[F, CommandSource],
    T: TextConversion[G],
    F: MonadError[G, NonEmptyList[CommandFailure]]
) extends KatLibCommands[F, G, Page, CommandSource, Player, User](FtoG)
    with SpongeBase[G]
    with SpongeValidators[G]
    with SpongeParameter[G]
    with SpongeOrParameter[G] { //We do not mix in the sponge help command
  import cats.instances.option._

  override def testPermission(command: SpongeCommandWrapper[G], source: CommandSource): G[Boolean] =
    command.testPermission(source).pure[G]

  override def commandUsage(command: SpongeCommandWrapper[G], source: CommandSource): G[Text] =
    T.spongeToOur(command.getUsage(source))

  override def commandHelp(command: SpongeCommandWrapper[G], source: CommandSource): G[Option[Text]] =
    command.getHelp(source).toOption.traverse(T.spongeToOur)

  override def commandDescription(command: SpongeCommandWrapper[G], source: CommandSource): G[Option[Text]] =
    command.getShortDescription(source).toOption.traverse(T.spongeToOur)

  override implicit val playerValidator: UserValidator[Player] = playerSender
  override implicit val userValidator:   UserValidator[User]   = userSender

  override implicit def commandOps(
      command: ComplexCommand
  ): CommandSyntax[G, CommandSource, Unit, Option[Location[World]], Int, SpongeCommandWrapper[G]] =
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
