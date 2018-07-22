package net.katsstuff.katlib.impl

import java.nio.file.Path

import cats.effect.IO
import net.katsstuff.katlib.KatPlugin
import net.katsstuff.katlib.algebras.PluginGlobal

class SpongePluginGlobalIO(implicit plugin: KatPlugin) extends PluginGlobal[IO] {

  override def id:          String = plugin.container.getId
  override def name:        String = plugin.container.getName
  override def version:     String = plugin.container.getVersion.orElse("")
  override def description: String = plugin.container.getDescription.orElse("")

  override def pluginDirectory: Path = plugin.configDir

  override def shiftSync:  IO[Unit] = IO.shift(plugin.syncExecutionContext)
  override def shiftAsync: IO[Unit] = IO.shift(plugin.asyncExecutionContext)
}
