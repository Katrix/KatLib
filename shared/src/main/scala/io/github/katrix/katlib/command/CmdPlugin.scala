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

import java.util.Locale

import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.helper.Implicits._
import io.github.katrix.katlib.i18n.{KLResource, Localized}

final class CmdPlugin(implicit plugin: KatPlugin) extends LocalizedCommand(None) {

  val cmdHelp       = new CmdHelp(this)
  var extraChildren = Seq.empty[CommandBase]

  override def execute(src: CommandSource, args: CommandContext): CommandResult = Localized(src) { implicit locale =>
    val container = plugin.container
    val text      = Text.builder(container.name).color(TextColors.YELLOW)
    container.version.foreach(version => text.append(t" v.$version"))
    container.description.foreach(description => text.append(Text.NEW_LINE).append(t"$description"))
    container.url.foreach(url => text.append(Text.NEW_LINE).append(t"$url"))
    if (container.authors.nonEmpty) text.append(KLResource.getText("cmd.plugin.createdBy", "creators" -> plugin.container.authors.mkString(", ")))

    src.sendMessage(text.build())
    CommandResult.success()
  }

  override def localizedDescription(implicit locale: Locale): Option[Text] = Some(KLResource.getText("cmd.plugin.description", "plugin" -> plugin.container.name))

  override def commandSpec: CommandSpec =
    CommandSpec
      .builder()
      .executor(this)
      .description(this)
      .permission(s"${plugin.container.id}.info")
      .children(this)
      .build()

  override def children: Seq[CommandBase] = cmdHelp +: extraChildren

  override def aliases: Seq[String] = Seq(s"${plugin.container.id}", s"${plugin.container.name}")
}
