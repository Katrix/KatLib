package net.katsstuff.katlib.executioncommands

import org.spongepowered.api.command.CommandCause

trait CauseValidator[A] {

  def validate(cause: CommandCause): Either[CommandFailureNEL, A]
}
