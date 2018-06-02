package net.katsstuff.katlib

import java.io.{File, InputStream, Reader}
import java.util.logging.{Level, Logger}

import org.bukkit.{OfflinePlayer, Server}
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.{PluginDescriptionFile, PluginLoader}
import org.bukkit.plugin.java.JavaPlugin

import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import net.katsstuff.katlib.commands.BukkitKatLibCommands
import net.katsstuff.katlib.impl._
import net.katsstuff.katlib.internal.KatLib
import net.katsstuff.scammander.CommandFailure
import net.katstuff.katlib.algebras._
import net.katstuff.katlib.impl.{FileAccessImpl, LocalizedImpl}

/**
  * A small convenience file to make stuff more scala like.
  */
class ScalaPluginIO extends JavaPlugin {

  implicit val plugin: ScalaPluginIO = this

  implicit val C:    Cache[IO]                                        = new BukkitCache[IO]
  implicit val CS:   CommandSourceAccess[IO, CommandSender]           = new BukkitCommandSourceOps[IO]
  implicit val FA:   FileAccess[IO]                                   = new FileAccessImpl[IO]
  implicit val L:    Localized[IO, CommandSender]                     = new LocalizedImpl[IO, CommandSender]
  implicit val Log:  LogHelper[IO]                                    = new BukkitLogHelper[IO]
  implicit val Page: Pagination.Aux[IO, CommandSender, List[PageOps]] = new BukkitPagination[IO](KatLib.newPages)
  implicit val P:    PlayerAccess[IO, Player, OfflinePlayer]          = new BukkitPlayerAccess[IO]
  implicit val G:    PluginGlobal[IO]                                 = new BukkitPluginGlobalIO

  def dataFolder:                                  File                  = getDataFolder
  def pluginLoader:                                PluginLoader          = getPluginLoader
  def server:                                      Server                = getServer
  protected def file:                              File                  = getFile
  def description:                                 PluginDescriptionFile = getDescription
  def config:                                      FileConfiguration     = getConfig
  def textResource(file: String):                  Reader                = getTextResource(file)
  def resource(filename: String):                  InputStream           = getResource(filename)
  final protected def classLoader:                 ClassLoader           = getClassLoader
  final protected def enabled_=(enabled: Boolean): Unit                  = setEnabled(enabled)
  final def naggable:                              Boolean               = isNaggable
  final def naggable_=(canNag: Boolean):           Unit                  = setNaggable(canNag)
  def logger:                                      Logger                = getLogger

  private def runEvent(event: IO[Unit], action: String): Unit =
    event.unsafeRunAsync(_.fold(e => logger.log(Level.SEVERE, s"Failed to $action plugin", e), identity))

  override final def onLoad(): Unit = runEvent(onLoadIO, "load")

  override final def onEnable(): Unit = runEvent(onEnableIO, "enable")

  override final def onDisable(): Unit = runEvent(onDisableIO, "disable")

  def onLoadIO: IO[Unit] = IO.unit

  def onEnableIO: IO[Unit] = IO.unit

  def onDisableIO: IO[Unit] = IO.unit

  def registerCommand(
      cake: BukkitKatLibCommands[IO, EitherT[IO, NonEmptyList[CommandFailure], ?], List[PageOps]]
  )(command: cake.Command[_, _], name: String): IO[Unit] = {
    import cake._
    IO(command.register(this, name))
  }

  def unregisterCommand(name: String): IO[Unit] =
    for {
      command <- IO(getCommand(name))
      _       <- IO(command.setExecutor(null))
      _       <- IO(command.setTabCompleter(null))
    } yield ()
}
