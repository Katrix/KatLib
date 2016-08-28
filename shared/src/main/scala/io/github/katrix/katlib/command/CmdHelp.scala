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
import scala.collection.mutable.ArrayBuffer

import org.spongepowered.api.Sponge
import org.spongepowered.api.command.args.{CommandContext, GenericArguments}
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.service.pagination.PaginationService
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.action.TextActions
import org.spongepowered.api.text.format.{TextColors, TextStyles}

import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.helper.Implicits._
import io.github.katrix.katlib.helper.LogHelper
import io.github.katrix.katlib.lib.LibCommonCommandKey

final class CmdHelp(cmdPlugin: CmdPlugin)(implicit plugin: KatPlugin) extends CommandBase(Some(cmdPlugin)) {

	private val registeredCommands = new ArrayBuffer[CommandBase]
	private val familyList         = new ArrayBuffer[Seq[CommandBase]]

	//We only compute the command aliases once, as they really shouldn't change, and it's a heavy computation for just getting help
	lazy private val commandsAliases = familyList.map(seq => {
		val reversed = seq.reverse
		val aliases = reversed.map(cmd => cmd.aliases)

		(toAliases(aliases).map(seq => seq.mkString(" ")), reversed.last)
	}).toMap.flatMap { case (seq, cmd) => (seq, Seq().padTo(seq.size, cmd)).zipped.toMap }

	private def toAliases[A](original: Seq[Seq[A]]): Seq[Seq[A]] = {
		val sizeNewList = original.map(_.size).product
		val originalSize = original.size

		@tailrec
		def inner(indexTop: Int, workingList: Seq[A], createdList: Seq[Seq[A]], count: Seq[Int]): Seq[Seq[A]] = {
			if(createdList.size >= sizeNewList) createdList
			else {
				val subList = original(indexTop)

				val currentCount = count(indexTop)
				val indexBottom = currentCount % subList.size
				val newWorkingList = workingList :+ subList(indexBottom)

				if(newWorkingList.size == originalSize) {
					inner(0, Seq(), createdList :+ newWorkingList, count.updated(indexTop, currentCount + 1))
				}
				else {
					inner(indexTop + 1, newWorkingList, createdList, count.updated(indexTop, currentCount + 1))
				}
			}
		}

		inner(0, Seq(), Seq(), Seq.fill(originalSize)(0))
	}

	def execute(src: CommandSource, args: CommandContext): CommandResult = {
		args.getOne[String](LibCommonCommandKey.Command).toOption match {
			case None =>
				val pages = Sponge.getGame.getServiceManager.provideUnchecked(classOf[PaginationService]).builder
				pages.title(s"${plugin.container.name} Help".richText.color(TextColors.RED).textOf)

				val text = registeredCommands.map(commandBase => getCommandHelp(commandBase, src))
				text.sorted
				pages.contents(text.asJavaCollection)
				pages.sendTo(src)
			case Some(commandName) => commandsAliases.get(commandName).map(_.commandSpec) match {
				case None =>
					src.sendMessage("Command not found".richText.error())
					return CommandResult.empty
				case Some(commandSpec) =>
					val commandText = Text.builder.append(s"/$commandName".richText.color(TextColors.GREEN).style(TextStyles.UNDERLINE).textOf)
					commandText.onHover(TextActions.showText(commandSpec.getHelp(src).orElse(commandSpec.getUsage(src))))
					commandText.append("\n".text, commandSpec.getHelp(src).orElse(commandSpec.getUsage(src)))
					src.sendMessage(commandText.build)
			}
		}

		CommandResult.success
	}

	def commandSpec: CommandSpec = CommandSpec.builder
		.description("This command right here.".text)
		.extendedDescription(s"Use /${plugin.container.id} help <command> <subcommand> \nto get help for a specific command".text)
		.permission(s"${plugin.container.id}.help")
		.arguments(GenericArguments.optional(GenericArguments.remainingJoinedStrings(LibCommonCommandKey.Command)))
		.executor(this)
		.build

	def aliases: Seq[String] = Seq("help")

	/**
		* Creates a written command that is what needs to be entered when
		* the specified command is passed in.
		*/
	private def stringCommand(command: CommandBase): String = {

		//StringBuilder here for performance
		@tailrec
		def inner(optParent: Option[CommandBase], builder: StringBuilder): String = optParent match {
			case None => builder.insert(0, '/').mkString.trim
			case Some(commandParent) => inner(commandParent.parent, builder.insert(0, s"${commandParent.aliases.head} "))
		}

		inner(Some(command), new StringBuilder)
	}

	/**
		* Creates a help [[Text]] based on the passed in command
		*/
	private def getCommandHelp(commandBase: CommandBase, src: CommandSource): Text = {
		val strCommand = stringCommand(commandBase)
		val commandSpec = commandBase.commandSpec

		val commandText = Text.builder().append(Text.of(TextColors.GREEN, TextStyles.UNDERLINE, strCommand))
		commandText.onHover(TextActions.showText(commandSpec.getHelp(src).orElse(commandSpec.getUsage(src))))
		commandText.onClick(TextActions.suggestCommand(strCommand))
		Text.of(commandText, " ", commandSpec.getShortDescription(src).orElse(commandSpec.getUsage(src)))
	}

	private[command] def registerCommandHelp(command: CommandBase) {

		@tailrec
		def commandFamily(optRelative: Option[CommandBase], currentFamily: Seq[CommandBase]): Seq[CommandBase] = optRelative match {
			case None => currentFamily
			case Some(relative) => commandFamily(relative.parent, currentFamily :+ relative)
		}

		val family = commandFamily(Some(command), Seq())
		LogHelper.trace(s"Registering help command: $family")
		registeredCommands += command
		familyList += family
	}
}