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

import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.helper.Implicits._

final class CmdPlugin(implicit plugin: KatPlugin) extends CommandBase(None) {

	val CmdHelp = new CmdHelp(this)

	override def execute(src: CommandSource, args: CommandContext): CommandResult = {
		val container = plugin.container
		val text = Text.builder(container.name).color(TextColors.YELLOW)
		container.version.foreach(version => text.append(s" v.$version".text))
		container.description.foreach(description => text.append(s"\n$description".text))

		src.sendMessage(text.build())
		CommandResult.success()
	}

	override def commandSpec: CommandSpec = {
		val builder = CommandSpec.builder()
			.description(s"Shows some information about ${plugin.container.name}".text)
			.executor(this)
			.permission(s"${plugin.container.id}.info")

		registerSubcommands(builder)

		builder.build()
	}

	override def children: Seq[CommandBase] = Seq(CmdHelp)

	override def aliases: Seq[String] = Seq(s"${plugin.container.id}")
}
