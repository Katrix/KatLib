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

import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}

import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.helper.Implicits._

abstract class CommandBase(val parent: Option[CommandBase])(implicit plugin: KatPlugin) extends CommandExecutor {

	def commandSpec: CommandSpec

	def aliases: Seq[String]

	def children: Seq[CommandBase] = Nil

	def registerHelp(): Unit = {
		plugin.pluginCmd.CmdHelp.registerCommandHelp(this)
		children.foreach(_.registerHelp())
	}

	protected def registerSubcommands(builder: CommandSpec.Builder): Unit = {
		children.foreach(command => builder.child(command.commandSpec, command.aliases: _*))
	}

	def nonPlayerError: CommandException = CommandBase.nonPlayerError
	def playerNotFoundError: CommandException = CommandBase.playerNotFoundError

	def notFoundError(notFound: String, lookFor: String): CommandException = CommandBase.notFoundError(notFound, lookFor)
	def invalidParameterError: CommandException = CommandBase.invalidParameterError
}

object CommandBase {

	def nonPlayerError: CommandException = new CommandException("Only players can use this command".richText.error())
	def playerNotFoundError: CommandException = notFoundError("A player", "with that name")

	def notFoundError(notFound: String, lookFor: String): CommandException = new CommandException(s"$notFound $lookFor was not found".richText.error())

	def invalidParameterError: CommandException = new CommandException("Invalid parameter".richText.error())

	implicit class RichCommandSpecBuilder(val builder: CommandSpec.Builder) extends AnyVal {

		def children(commandBase: CommandBase): CommandSpec.Builder = {
			commandBase.registerSubcommands(builder)
			builder
		}
	}
}