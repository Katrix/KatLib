package net.katsstuff.katlib.impl

import java.nio.file.Path

import org.bukkit.Bukkit

import cats.effect.IO
import net.katsstuff.katlib.ScalaPluginIO
import net.katstuff.katlib.algebras.PluginGlobal

class BukkitPluginGlobalIO(implicit plugin: ScalaPluginIO) extends PluginGlobal[IO] {

  override def id:          String = plugin.description.getName
  override def name:        String = plugin.description.getName
  override def version:     String = plugin.description.getVersion
  override def description: String = Option(plugin.description.getDescription).getOrElse("")

  override def pluginDirectory: Path = plugin.dataFolder.toPath

  override def shiftSync: IO[Unit] = IO.async { cb =>
    Bukkit.getScheduler.runTask(plugin, new Runnable {
      def run(): Unit = cb(Right(()))
    })
  }

  override def shiftAsync: IO[Unit] = IO.async { cb =>
    Bukkit.getScheduler.runTaskAsynchronously(plugin, new Runnable {
      def run(): Unit = cb(Right(()))
    })
  }
}
