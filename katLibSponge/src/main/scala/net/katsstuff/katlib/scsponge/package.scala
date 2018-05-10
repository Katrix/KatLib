package net.katsstuff.katlib

import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.function.BiFunction

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.util.Try

import org.slf4j.Logger
import org.spongepowered.api.asset.Asset
import org.spongepowered.api.Sponge
import org.spongepowered.api.block.{BlockState, BlockType}
import org.spongepowered.api.block.tileentity.TileEntity
import org.spongepowered.api.entity.Entity
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.world.{LocatableBlock, World}
import org.spongepowered.api.world.extent.Extent

import com.flowpowered.math.vector.{Vector3d, Vector3i}

import net.katsstuff.katlib.helper.Implicits._

package object scsponge {

  //TODO: PropertyRegistry
  //TODO: DataManager
  //TODO: DataManager
  //TODO: GameRegistry
  //TODO: Game
  //TODO: Server
  //TODO: PluginManager
  //TODO: CommandManager
  //TODO: ServiceManager
  //TODO: Scheduler
  //TODO: BlockState
  //TODO: BlockType
  //TODO: TileEntity
  //TODO: Entity
  //TODO: Find better way to handle cause
  //TODO: LocatableBlock
  //TODO: World
  //TODO: Extent
  //TODO: Vector3d
  //TODO: Vector3i

  type PlatformType = org.spongepowered.api.Platform.Type
  object PlatformType {
    val Client  = org.spongepowered.api.Platform.Type.CLIENT
    val Server  = org.spongepowered.api.Platform.Type.SERVER
    val Unknown = org.spongepowered.api.Platform.Type.UNKNOWN
  }
  type PlatformComponent = org.spongepowered.api.Platform.Component
  object PlatformComponent {
    val Game           = org.spongepowered.api.Platform.Component.GAME
    val Api            = org.spongepowered.api.Platform.Component.API
    val Implementation = org.spongepowered.api.Platform.Component.IMPLEMENTATION
  }

  type Platform = org.spongepowered.api.Platform
  def Platform: Platform = Sponge.getPlatform
  implicit class PlatformSyntax(private val platform: Platform) extends AnyVal {

    def tpe: PlatformType = platform.getType

    def executionType: PlatformType = platform.getExecutionType

    def container(component: PlatformComponent): PluginContainer = platform.getContainer(component)

    def minecraftVersion: MinecraftVersion = platform.getMinecraftVersion

    def asMap: mutable.Map[String, AnyRef] = platform.asMap.asScala
  }

  type PluginContainer = org.spongepowered.api.plugin.PluginContainer
  implicit class PluginContainerSyntax(val container: PluginContainer) extends AnyVal {

    def id: String = container.getId

    def name: String = container.getName

    def version: Option[String] = container.getVersion.toOption

    def description: Option[String] = container.getDescription.toOption

    def url: Option[String] = container.getUrl.toOption

    def authors: Seq[String] = container.getAuthors.asScala

    def getAsset(name: String): Option[Asset] = container.getAsset(name).toOption

    def sources: Option[Path] = container.getSource.toOption

    def instance: Option[_] = container.getInstance().toOption

    def logger: Logger = container.getLogger
  }

  type MinecraftVersion = org.spongepowered.api.MinecraftVersion
  implicit class MinecraftVersionSyntax(private val version: MinecraftVersion) extends AnyVal {
    def name: String = version.getName
  }

  type AssetManager = org.spongepowered.api.asset.AssetManager
  def AssetManager: AssetManager = Sponge.getAssetManager

  implicit class AssetManagerSyntax(private val assets: AssetManager) extends AnyVal {

    def getAsset(plugin: Any, name: String): Option[Asset] = assets.getAsset(plugin, name).toOption

    def getAsset(name: String): Option[Asset] = assets.getAsset(name).toOption
  }

  type Asset = org.spongepowered.api.asset.Asset
  implicit class AssetSyntax(private val asset: Asset) extends AnyVal {

    def owner: PluginContainer = asset.getOwner

    def url: URL = asset.getUrl

    def tryCopyToFile(output: Path, overwrite: Boolean = false, onlyIfAbsent: Boolean = true): Try[Unit] =
      Try(asset.copyToFile(output, overwrite, onlyIfAbsent))

    def tryCopyToDirectory(outputDirectory: Path, overwrite: Boolean = false, onlyIfAbsent: Boolean = true): Try[Unit] =
      Try(asset.copyToDirectory(outputDirectory, overwrite, onlyIfAbsent))

    def fileName: String = asset.getFileName

    def tryReadString(charset: Charset = Asset.DEFAULT_CHARSET): Try[String] = Try(asset.readString(charset))

    def tryReadLines(charset: Charset = Asset.DEFAULT_CHARSET): Try[mutable.Buffer[String]] =
      Try(asset.readLines(charset).asScala)

    def tryReadBytes(): Try[Array[Byte]] = Try(asset.readBytes())
  }

  type ConfigManager = org.spongepowered.api.config.ConfigManager
  def ConfigManager: ConfigManager = Sponge.getGame.getConfigManager

  type EventManager = org.spongepowered.api.event.EventManager
  def EventManager: EventManager = Sponge.getEventManager

  implicit class EventManagerSyntax(private val events: EventManager) extends AnyVal {

    def registerListener[A <: Event](
        plugin: Any,
        listener: EventListener[_ >: A <: Event],
        order: EventOrder = EventOrder.Default,
        beforeModifications: Boolean = false,
    )(implicit classTag: ClassTag[A]): Unit =
      events.registerListener(
        plugin,
        classTag.runtimeClass.asInstanceOf[Class[A]],
        order,
        beforeModifications,
        listener
      )

    def registerListenerFunc[A <: Event](
        plugin: Any,
        listener: (_ >: A <: Event) => Unit,
        order: EventOrder = EventOrder.Default,
        beforeModifications: Boolean = false,
    )(implicit classTag: ClassTag[A]): Unit =
      events.registerListener(
        plugin,
        classTag.runtimeClass.asInstanceOf[Class[A]],
        order,
        beforeModifications,
        (event: A) => listener(event)
      )
  }

  type Event                     = org.spongepowered.api.event.Event
  type EventListener[A <: Event] = org.spongepowered.api.event.EventListener[A]

  type EventOrder = org.spongepowered.api.event.Order
  object EventOrder {
    import org.spongepowered.api.event.{Order => SpongeOrder}
    val Pre        = SpongeOrder.PRE
    val AfterPre   = SpongeOrder.AFTER_PRE
    val First      = SpongeOrder.FIRST
    val Early      = SpongeOrder.EARLY
    val Default    = SpongeOrder.DEFAULT
    val Late       = SpongeOrder.LATE
    val Last       = SpongeOrder.LAST
    val BeforePost = SpongeOrder.BEFORE_POST
    val Post       = SpongeOrder.POST
  }

  type ChannelRegistrar = org.spongepowered.api.network.ChannelRegistrar
  def ChannelRegistrar: ChannelRegistrar = Sponge.getChannelRegistrar

  type TeleportHelper = org.spongepowered.api.world.TeleportHelper
  implicit class TeleportHelperSyntax(helper: TeleportHelper) {

    def getSafeLocation(location: Location[World]): Option[Location[World]] =
      helper.getSafeLocation(location).toOption

    def getSafeLocation(location: Location[World], height: Int, width: Int): Option[Location[World]] =
      helper.getSafeLocation(location, height, width).toOption
  }
  def TeleportHelper: TeleportHelper = Sponge.getGame.getTeleportHelper

  type Location[E <: Extent] = org.spongepowered.api.world.Location[E]
  implicit class LocationSyntax[E <: Extent](private val loc: Location[E]) extends AnyVal {

    def extent: E = loc.getExtent

    def position: Vector3d = loc.getPosition

    def blockPosition: Vector3i = loc.getBlockPosition

    def chunkPosition: Vector3i = loc.getChunkPosition

    def biomePosition: Vector3i = loc.getBiomePosition

    def x: Double = loc.getX
    def y: Double = loc.getY
    def z: Double = loc.getZ

    def blockX: Int = loc.getBlockX
    def blockY: Int = loc.getBlockY
    def blockZ: Int = loc.getBlockZ

    def getLocatableBlock: Option[LocatableBlock] = loc.getLocatableBlock.toOption

    def map[A](f: (E, Vector3d) => A): A = {
      val jFunc: BiFunction[E, Vector3d, A] = (e, pos) => f(e, pos)
      loc.map(jFunc)
    }

    def mapBlock[A](f: (E, Vector3i) => A): A = {
      val jFunc: BiFunction[E, Vector3i, A] = (e, pos) => f(e, pos)
      loc.mapBlock(jFunc)
    }

    def mapChunk[A](f: (E, Vector3i) => A): A = {
      val jFunc: BiFunction[E, Vector3i, A] = (e, pos) => f(e, pos)
      loc.mapChunk(jFunc)
    }

    def mapBiome[A](f: (E, Vector3i) => A): A = {
      val jFunc: BiFunction[E, Vector3i, A] = (e, pos) => f(e, pos)
      loc.mapBiome(jFunc)
    }

    def blockType: BlockType = loc.getBlockType

    def block: BlockState = loc.getBlock

    def tileEntity: Option[TileEntity] = loc.getTileEntity.toOption

    def spawnEntities(entities: Iterable[_ <: Entity], cause: Cause): Boolean =
      loc.spawnEntities(entities.asJava, cause)
  }
}
