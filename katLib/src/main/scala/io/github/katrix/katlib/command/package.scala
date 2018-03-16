package io.github.katrix.katlib

import org.spongepowered.api.command.CommandMapping
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

import io.github.katrix.katlib.i18n.{KLResource, Localized}
import net.katsstuff.scammander.sponge._
import io.github.katrix.katlib.helper.Implicits._

package object command {

  def registerPluginCommand(childSet: Set[ChildCommand[_, _]])(implicit plugin: KatPlugin): Option[CommandMapping] =
    Command
      .withChildren[NotUsed](childSet) { (src, _, _) =>
        Localized(src) { implicit locale =>
          val container = plugin.container
          val text      = Text.builder(container.name).color(TextColors.YELLOW)

          container.version.foreach(version => text.append(t" v.$version"))
          container.description.foreach(description => text.append(Text.NEW_LINE, t"$description"))
          container.url.foreach(url => text.append(Text.NEW_LINE, t"$url"))
          if (container.authors.nonEmpty) {
            text.append(
              Text.NEW_LINE,
              KLResource.getText("cmd.plugin.createdBy", "creators" -> plugin.container.authors.mkString(", "))
            )
          }

          src.sendMessage(text.build())
          Command.successStep()
        }
      }
      .register(
        plugin,
        Alias(s"${plugin.container.id}", s"${plugin.container.name}"),
        Permission(s"${plugin.container.id}.info"),
        shortDescription = Description { src =>
          Localized(src) { implicit locale =>
            KLResource.getText("cmd.plugin.description", "plugin" -> plugin.container.name)
          }
        },
      )

}
