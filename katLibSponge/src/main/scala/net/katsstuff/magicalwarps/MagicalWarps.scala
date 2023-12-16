package net.katsstuff.magicalwarps

import net.katsstuff.katlib.KatLibPlugin
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.Command
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent
import org.spongepowered.plugin.jvm.Plugin

@Plugin("magicalwarps")
object MagicalWarps extends KatLibPlugin:
  override def usesSharedConfig: Boolean = false

  @Listener
  def onRegisterCommands(event: RegisterCommandEvent[Command.Parameterized]): Unit =
    ???
