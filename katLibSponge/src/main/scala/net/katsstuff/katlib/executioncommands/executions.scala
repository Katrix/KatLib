package net.katsstuff.katlib.executioncommands

import cats.data.{NonEmptyList, ValidatedNel}
import cats.syntax.all._
import net.katsstuff.minejson.text.Text
import org.spongepowered.api.command.registrar.tree.CommandTreeNode
import org.spongepowered.api.command.{CommandCause, CommandResult}

sealed trait Executions:
  
  def handleCommand(cause: CommandCause, arguments: String): ValidatedNel[(CommandResult, CommandFailureNEL, Usage), CommandResult]
  
  def tabComplete(cause: CommandCause, arguments: String): Seq[String]
  
  def addArgFirst[Arg](parameter: Parameter[Arg]): Executions
  
  def flatten: Executions
  
  def children: NonEmptyList[Executions]
end Executions

case class AggExecutions(executions: NonEmptyList[Executions]) extends Executions:
  override def handleCommand(cause: CommandCause, arguments: String): ValidatedNel[(CommandResult, CommandFailureNEL, Usage), CommandResult] =
    executions.reduceLeftTo(_.handleCommand(cause, arguments))((res, ex) => res.findValid(ex.handleCommand(cause, arguments)))

  def tabComplete(cause: CommandCause, arguments: String): Seq[String] =
    executions.toList.flatMap(_.tabComplete(cause, arguments))

  override def flatten: Executions = AggExecutions(children)

  override def children: NonEmptyList[Executions] = executions.flatMap(_.children)

  override def addArgFirst[Arg](parameter: Parameter[Arg]): Executions = 
    AggExecutions(executions.map(_.addArgFirst(parameter)))
end AggExecutions

case class RunExecution[AllArgs, RunArgs, Cause](
    permission: Option[String],
    shortDescription: CommandCause => Option[Text],
    extendedDescription: CommandCause => Option[Text],
    help: CommandCause => Option[Text],
    causeValidator: CauseValidator[Cause],
    allArgs: Parameter[AllArgs],
    allArgsToRunArgs: AllArgs => RunArgs,
    run: (Cause, RunArgs) => Either[String, CommandResult]
) extends Executions:
  override def handleCommand(cause: CommandCause, arguments: String) = ???

  override def tabComplete(cause: CommandCause, arguments: String): Seq[String] =
    if permission.forall(cause.hasPermission) then
      ???
    else
      Nil

  override def addArgFirst[Arg](parameter: Parameter[Arg]): Executions =
    new RunExecution[(Arg, AllArgs), RunArgs, Cause](
      permission,
      shortDescription,
      extendedDescription,
      help,
      causeValidator,
      parameter ~ allArgs,
      t => allArgsToRunArgs(t._2),
      run
    )

  override def flatten: Executions = this

  override def children: NonEmptyList[Executions] = NonEmptyList.one(this)
end RunExecution
