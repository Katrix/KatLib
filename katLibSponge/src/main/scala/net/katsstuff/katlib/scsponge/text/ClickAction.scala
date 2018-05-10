package net.katsstuff.katlib.scsponge.text

import java.net.URL

import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.text.action.{TextActions, ClickAction => SpongeClickAction}

object ClickAction {

  def openUrl(url: URL): SpongeClickAction.OpenUrl = TextActions.openUrl(url)

  def runCommand(command: String): SpongeClickAction.RunCommand = TextActions.runCommand(command)

  def changePage(page: Int): SpongeClickAction.ChangePage = TextActions.changePage(page)

  def suggestCommand(command: String): SpongeClickAction.SuggestCommand = TextActions.suggestCommand(command)

  def executeCallback(f: CommandSource => Unit): SpongeClickAction.ExecuteCallback =
    TextActions.executeCallback((src: CommandSource) => f(src))
}
