package io.github.katrix.katlib.command

import java.util.Locale

import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.text.Text

import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.i18n.Localized

abstract class LocalizedCommand(parent: Option[CommandBase])(implicit plugin: KatPlugin) extends CommandBase(parent) {

  def localizedDescription(implicit locale: Locale):         Option[Text] = None
  def localizedExtendedDescription(implicit locale: Locale): Option[Text] = None

  override def description(src: CommandSource):         Option[Text] = Localized(src)(implicit locale => localizedDescription)
  override def extendedDescription(src: CommandSource): Option[Text] = Localized(src)(implicit locale => localizedExtendedDescription)
}

object LocalizedCommand {

  implicit class RichCommandSpecBuilder(val builder: CommandSpec.Builder) extends AnyVal {

    def children(commandBase: LocalizedCommand): CommandSpec.Builder = {
      commandBase.registerSubcommands(builder)
      builder
    }

    def description(command: LocalizedCommand): CommandSpec.Builder =
      builder.description(command.localizedDescription(Localized.Default).orNull)
    def extendedDescription(command: LocalizedCommand): CommandSpec.Builder =
      builder.extendedDescription(command.localizedExtendedDescription(Localized.Default).orNull)
  }
}
