package net.katsstuff.katlib.internal

import java.util.UUID

import org.bukkit.command.CommandSender

import cats.instances.list._
import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import net.katsstuff.katlib.ScalaPluginIO
import net.katsstuff.katlib.impl.PageOps
import net.katsstuff.katlib.internal.commands.KatLibCommandBundle
import net.katsstuff.minejson.text.Text
import net.katsstuff.scammander.CommandFailure

object KatLibPlugin {
  private var _newPages: (CommandSender, UUID => Seq[Text]) => IO[Text] = _

  def newPages: (CommandSender, UUID => Seq[Text]) => IO[Text] = _newPages
}
class KatLibPlugin extends ScalaPluginIO {

  private val katLibCommands = new KatLibCommandBundle[IO, EitherT[IO, NonEmptyList[CommandFailure], ?], List[PageOps]](
    EitherT.liftK[IO, NonEmptyList[CommandFailure]]
  ) {
    override protected def runComputation[A](
      computation: EitherT[IO, CommandFailureNEL, A]
    ): Either[CommandFailureNEL, A] = computation.value.unsafeRunSync()
  }

  val newPages: (CommandSender, UUID => Seq[Text]) => IO[Text] = katLibCommands.PageCmd.newPages(_, _)

  override val onEnableIO: IO[Unit] =
    for {
      _ <- IO(KatLibPlugin._newPages = newPages)
      _ <- registerCommand(katLibCommands)(katLibCommands.PageCmd, "page")
      _ <- registerCommand(katLibCommands)(katLibCommands.CallbackCmd, "callback")
    } yield ()

  override val onDisableIO: IO[Unit] =
    for {
      _ <- IO(KatLibPlugin._newPages = null)
      _ <- unregisterCommand("page")
      _ <- unregisterCommand("callback")
    } yield ()
}
