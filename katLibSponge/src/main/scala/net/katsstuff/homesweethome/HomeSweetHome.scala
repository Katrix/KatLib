package net.katsstuff.homesweethome

import net.katsstuff.katlib.KatLibPlugin
import net.katsstuff.katlib.helpers._
import org.spongepowered.api.command.{Command, CommandResult}
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.lifecycle.{LoadedGameEvent, RefreshGameEvent, RegisterCommandEvent}
import org.spongepowered.plugin.jvm.Plugin

import scala.util.{Failure, Success}
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import cats.effect.{Blocker, ContextShift, IO, Resource}
import net.katsstuff.minejson.text.Text
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.identity.Identity
import org.spongepowered.api.{ResourceKey, Sponge}
import org.spongepowered.api.entity.Entity
import org.spongepowered.api.service.permission.Subject
import org.spongepowered.api.world.server.ServerLocation
import org.spongepowered.math.vector.Vector3d

import java.nio.file.Paths
import java.util.UUID
import doobie._

import scala.concurrent.ExecutionContext
import scala.sys.ShutdownHookThread

@Plugin("homesweethome")
object HomeSweetHome extends KatLibPlugin:
  
  private var _config: HomeConfig = null
  def config: HomeConfig = _config

  override def usesSharedConfig: Boolean = false
  
  given Meta[ResourceKey] = Meta[String].timap(ResourceKey.resolve)(_.getFormatted)
  given Meta[Array[UUID]] = Meta.Advanced.other[Array[UUID]]("ARRAY")
  
  given homeHandler: HomeHandler[Subject, ResourceKey] = HomeHandler((subject, option) => subject.getOption(option).toScala)
  
  given ContextShift[IO] = IO.contextShift(summon[ExecutionContext])
  
  given Transactor[IO] = runResource {
    val sqlUrl =
      if Sponge.getSqlManager.getConnectionUrlFromAlias("homesweethome").isPresent then
        "homesweethome"
      else
        "jdbc:h2:homes.db"
        
    val homeDataSource = Sponge.getSqlManager.getDataSource(container, sqlUrl)
    
    for
      ce <- ExecutionContexts.fixedThreadPool[IO](32) 
      be <- Blocker[IO]
    yield Transactor.fromDataSource[IO](homeDataSource, ce, be)
  }
  
  @Listener
  def onRegisterCommands(event: RegisterCommandEvent[Command.Parameterized]): Unit =
    given cmd.SpongeHomeCmdImpl with
      val CmdSuccess: CommandResult = CommandResult.success
      
      def audienceFromId(identity: Identity): Audience =
        Sponge
          .getServer
          .getPlayer(identity.uuid)
          .toScala
          .getOrElse(Audience.empty)

      def subjectFromId(identity: Identity): Subject =
        Sponge
          .getServer
          .getUserManager
          .get(identity.uuid)
          .toScala
          .getOrElse(Sponge.getServer.getServiceProvider.permissionService.getDefaults)

      def nameFromUUID(uuid: UUID): String =
        Sponge
          .getServer
          .getUserManager
          .get(uuid)
          .toScala
          .fold(uuid.toString)(_.getName)
      
      def isIdPresent(identity: Identity): Boolean =
        Sponge.getServer.getPlayer(identity.uuid).isPresent
      
      def makeHome(location: ServerLocation, rotation: Vector3d): Home[ResourceKey] =
        Home(
          location.getX, 
          location.getY, 
          location.getZ, 
          rotation.getX, 
          rotation.getY, 
          location.getWorldKey, 
          Vector.empty
        )
      
      def reloadData(): Either[Throwable, Unit] =
        HomeConfig.load(configRoot.getConfigPath).toEither.map(config => _config = config)
      
      def sendPagination(title: Text, content: Seq[Text], sendTo: Audience): Unit =
        Sponge
          .getGame
          .getServiceProvider
          .paginationService
          .builder
          .title(title.toSponge)
          .contents(content.map(_.toSponge).asJava)
          .sendTo(sendTo)
      
      def teleport(home: Home[ResourceKey], teleportable: Entity): Boolean =
        val location = ServerLocation.of(home.worldId, home.x, home.y, home.z)
        val rotation = Vector3d(home.yaw, home.pitch, 0)

        teleportable.setLocationAndRotation(location, rotation)
    
    cmd.homeCommand.register(event)
    cmd.homesCommand.register(event)

  @Listener
  def onStart(event: LoadedGameEvent): Unit =
    event.getGame.getEventManager.registerListeners(container, homeHandler)
    reloadConfig()
  
    OldHome
      .loadOldHomes(configRoot.getDirectory.resolve("storage.json"))
      .foreach(homeHandler.transferOldHomes)

  @Listener
  def onReload(event: RefreshGameEvent): Unit = 
    reloadConfig()
    homeHandler.refreshCache(Sponge.getServer.getOnlinePlayers.asScala.map(_.identity))

  def reloadConfig(): Unit =
    HomeConfig.load(configRoot.getConfigPath) match
      case Success(config) => _config = config
      case Failure(e) => 
        logger.error("Couldn't load config", e)
        
  private def runResource[A](resource: Resource[IO, A]): A = 
    val (obj, release) = resource.allocated.unsafeRunSync()
    ShutdownHookThread(release.unsafeRunSync())
    obj
