package net.katsstuff.katlib.executioncommands

import cats.data.NonEmptyList

type CommandFailureNEL = NonEmptyList[CommandFailure]

sealed trait CommandFailure {
  def msg: String

  def shouldShowUsage: Boolean
}

case class CommandError(msg: String, shouldShowUsage: Boolean = false) extends CommandFailure
case class CommandSyntaxError(msg: String, pos: Int) extends CommandFailure {
  override def shouldShowUsage: Boolean = true
}
case class CommandUsageError(msg: String, pos: Int) extends CommandFailure {
  override def shouldShowUsage: Boolean = false
}
