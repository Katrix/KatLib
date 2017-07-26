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

import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}
import org.spongepowered.api.command.{CommandException, CommandSource}
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors._

import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.helper.Implicits._
import io.github.katrix.katlib.i18n.{KLResource, Localized}

abstract class CommandBase(val parent: Option[CommandBase])(implicit plugin: KatPlugin) extends CommandExecutor {

  def commandSpec: CommandSpec

  def aliases: Seq[String]

  def children: Seq[CommandBase] = Nil

  def description(src: CommandSource): Option[Text] = commandSpec.getShortDescription(src).toOption
  def extendedDescription(src: CommandSource): Option[Text] = Option(CommandBase.extendedDescriptionField.get(commandSpec).asInstanceOf[Text])
  def usage(src: CommandSource): Text = commandSpec.getUsage(src)

  def help(src: CommandSource): Text = {
    val builder = Text.builder
    val desc = description(src)
    desc.foreach(builder.append(_, Text.NEW_LINE))
    builder.append(usage(src))
    extendedDescription(src).foreach(builder.append(Text.NEW_LINE, _))
    builder.build
  }

  def registerHelp(): Unit = {
    plugin.pluginCmd.cmdHelp.registerCommandHelp(this)
    children.foreach(_.registerHelp())
  }

  protected def registerSubcommands(builder: CommandSpec.Builder): Unit =
    children.foreach(command => builder.child(command.commandSpec, command.aliases: _*))

  def nonPlayerErrorLocalized(implicit locale: Locale): CommandException = new CommandException(t"$RED${KLResource.get("command.error.nonPlayer")}")
  def playerNotFoundErrorLocalized(implicit locale: Locale): CommandException =
    new CommandException(t"$RED${KLResource.get("command.error.playerNotFound")}")
  def invalidParameterErrorLocalized(implicit locale: Locale): CommandException =
    new CommandException(t"$RED${KLResource.get("command.error.invalidParameter")}")

  @Deprecated
  def nonPlayerError: CommandException = CommandBase.nonPlayerError
  @Deprecated
  def playerNotFoundError: CommandException = CommandBase.playerNotFoundError
  @Deprecated
  def notFoundError(notFound: String, lookFor: String): CommandException = CommandBase.notFoundError(notFound, lookFor)
  @Deprecated
  def invalidParameterError: CommandException = CommandBase.invalidParameterError
}

object CommandBase {

  private val extendedDescriptionField = classOf[CommandSpec].getField("extendedDescription")

  def nonPlayerErrorLocalized(implicit locale: Locale): CommandException = new CommandException(t"$RED${KLResource.get("command.error.nonPlayer")}")
  def playerNotFoundErrorLocalized(implicit locale: Locale): CommandException =
    new CommandException(t"$RED${KLResource.get("command.error.playerNotFound")}")
  def invalidParameterErrorLocalized(implicit locale: Locale): CommandException =
    new CommandException(t"$RED${KLResource.get("command.error.invalidParameter")}")

  @Deprecated
  def nonPlayerError: CommandException = nonPlayerErrorLocalized(Localized.Default)
  @Deprecated
  def playerNotFoundError: CommandException = playerNotFoundErrorLocalized(Localized.Default)
  @Deprecated
  def notFoundError(notFound: String, lookFor: String): CommandException = new CommandException(t"$RED$notFound $lookFor was not found")
  @Deprecated
  def invalidParameterError: CommandException = invalidParameterErrorLocalized(Localized.Default)

  implicit class RichCommandSpecBuilder(val builder: CommandSpec.Builder) extends AnyVal {

    def children(commandBase: CommandBase): CommandSpec.Builder = {
      commandBase.registerSubcommands(builder)
      builder
    }
  }
}
