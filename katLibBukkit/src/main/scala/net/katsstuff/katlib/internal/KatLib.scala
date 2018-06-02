package net.katsstuff.katlib.internal

import java.util.UUID

import org.bukkit.command.CommandSender

import cats.arrow.FunctionK
import cats.instances.list._
import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import net.katsstuff.katlib.ScalaPluginIO
import net.katsstuff.katlib.impl.PageOps
import net.katsstuff.katlib.internal.commands.KatLibCommandBundle
import net.katsstuff.minejson.text.Text
import net.katsstuff.scammander.CommandFailure

object KatLib extends ScalaPluginIO {

  private val katLibCommands = new KatLibCommandBundle[IO, EitherT[IO, NonEmptyList[CommandFailure], ?], List[PageOps]](
    new FunctionK[IO, EitherT[IO, NonEmptyList[CommandFailure], ?]] {
      override def apply[A](fa: IO[A]): EitherT[IO, NonEmptyList[CommandFailure], A] =
        EitherT.right[NonEmptyList[CommandFailure]](fa)
    }
  ) {
    override protected def runComputation[A](
        computation: EitherT[IO, CommandFailureNEL, A]
    ): Either[CommandFailureNEL, A] = computation.value.unsafeRunSync()
  }

  val newPages: (CommandSender, UUID => Seq[Text]) => IO[Text] = katLibCommands.PageCmd.newPages(_, _)

  override val onEnableIO: IO[Unit] =
    for {
      _ <- registerCommand(katLibCommands)(katLibCommands.PageCmd, "page")
      _ <- registerCommand(katLibCommands)(katLibCommands.CallbackCmd, "callback")
    } yield ()

  override val onDisableIO: IO[Unit] =
    for {
      _ <- unregisterCommand("page")
      _ <- unregisterCommand("callback")
    } yield ()
}
