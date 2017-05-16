/*
 * This file is part of KatLib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 Katrix
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.katrix.katlib.command

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable

import org.spongepowered.api.Sponge
import org.spongepowered.api.command.args.{CommandContext, GenericArguments}
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.command.{CommandException, CommandResult, CommandSource}
import org.spongepowered.api.service.pagination.PaginationService
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.action.TextActions
import org.spongepowered.api.text.format.TextColors._
import org.spongepowered.api.text.format.{TextColors, TextStyles}

import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.helper.Implicits._
import io.github.katrix.katlib.helper.LogHelper
import io.github.katrix.katlib.lib.LibCommonTCommandKey

final class CmdHelp(cmdPlugin: CmdPlugin)(implicit plugin: KatPlugin) extends CommandBase(Some(cmdPlugin)) {

  private val commandParents = new mutable.HashMap[CommandBase, Seq[CommandBase]]

  lazy private val commandsAliases = commandParents.flatMap { case (topCmd, xs) =>
    val named = xs.map(_.aliases)

    val allAliases = named.reduce { (names, acc) =>
      for {
        cmdName <- names
        str <- acc
      } yield s"$cmdName $str"
    }.sorted

    allAliases.map(_ -> topCmd)
  }

  def execute(src: CommandSource, args: CommandContext): CommandResult = {
    args.one(LibCommonTCommandKey.Command) match {
      case None =>
        val pages = Sponge.getGame.getServiceManager.provideUnchecked(classOf[PaginationService]).builder
        pages.title(t"$RED${plugin.container.name} Help")

        val text = commandParents.keys.toSeq.flatMap(commandBase => getCommandHelp(commandBase, src)).sorted
        pages.contents(text.asJavaCollection)
        pages.sendTo(src)
        CommandResult.success()
      case Some(commandName) =>
        val data = for {
          cmd <- commandsAliases.get(commandName).toRight(new CommandException(t"${RED}Command not found"))
          help <- getCommandHelp(cmd, src).toRight(new CommandException(t"${RED}Couldn't find any help for that command"))
        } yield help

        data match {
          case Right(help) =>
            src.sendMessage(help)
            CommandResult.success()
          case Left(e) => throw e
        }
    }
  }

  def commandSpec: CommandSpec =
    CommandSpec.builder
      .description(t"This command right here.")
      .extendedDescription(t"Use /${plugin.container.id} help <command> <subcommand> \nto get help for a specific command")
      .permission(s"${plugin.container.id}.help")
      .arguments(GenericArguments.optional(GenericArguments.remainingJoinedStrings(LibCommonTCommandKey.Command)))
      .executor(this)
      .build

  def aliases: Seq[String] = Seq("help")

  /**
		* Creates a written command that is what needs to be entered when
		* the specified command is passed in.
		*/
  private def stringCommand(command: CommandBase): Option[String] =
    commandParents.get(command).map(seq => s"/${seq.map(_.aliases.head).mkString(" ")}")

  /**
		* Creates a help [[Text]] based on the passed in command
		*/
  private def getCommandHelp(commandBase: CommandBase, src: CommandSource): Option[Text] = {
    stringCommand(commandBase).map { strCommand =>
      val commandSpec = commandBase.commandSpec

      val commandText = Text.builder().append(Text.of(TextColors.GREEN, TextStyles.UNDERLINE, strCommand))
      commandText.onHover(TextActions.showText(commandSpec.getHelp(src).orElse(commandSpec.getUsage(src))))
      commandText.onClick(TextActions.suggestCommand(strCommand))
      Text.of(commandText, " ", commandSpec.getShortDescription(src).orElse(commandSpec.getUsage(src)))
    }
  }

  private[command] def registerCommandHelp(command: CommandBase) {

    @tailrec
    def commandParent(optParent: Option[CommandBase], acc: List[CommandBase]): List[CommandBase] = optParent match {
      case Some(cmdParent) => commandParent(cmdParent.parent, cmdParent :: acc)
      case None => acc
    }

    val parents = commandParent(Some(command), Nil)
    LogHelper.trace(s"Registering help command: $parents")
    commandParents.put(command, parents)
  }
}
