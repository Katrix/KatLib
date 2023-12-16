package net.katsstuff.homesweethome

import com.github.benmanes.caffeine.cache.Caffeine
import net.katsstuff.homesweethome.lib.LibPerm
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.network.ServerSideConnectionEvent

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import scala.jdk.FutureConverters._
import cats.effect.IO
import net.kyori.adventure.identity.{Identified, Identity}

import doobie._
import doobie.implicits._
import doobie.h2.implicits._

class HomeHandler[Subject, WorldId](
  getOptionFromSubject: (Subject, String) => Option[String]
)(using ExecutionContext, Meta[WorldId], Meta[Array[UUID]])(using xa: Transactor[IO]):
  
  private val homeCache: mutable.Map[UUID, Map[String, Home[WorldId]]] = TrieMap.empty
  private val requests: mutable.Map[UUID, mutable.Map[UUID, Home[WorldId]]] = mutable.Map.empty
  private val invites: mutable.Map[UUID, mutable.Map[UUID, Home[WorldId]]] = mutable.Map.empty
  
  private val tempHomeCache: mutable.Map[UUID, Map[String, Home[WorldId]]] =
    Caffeine
      .newBuilder()
      .expireAfterAccess(30, TimeUnit.SECONDS)
      .buildAsync[UUID, Map[String, Home[WorldId]]] { (key, executor) => 
        allHomesForUUIDFromDb(key).unsafeToFuture().asJava.toCompletableFuture
      }
      .synchronous
      .asMap
      .asScala

  private def createInvitesRequestsMap: mutable.Map[UUID, Home[WorldId]] =
    Caffeine
      .newBuilder()
      .expireAfterWrite(HomeSweetHome.config.timeoutDuration.toSeconds, TimeUnit.SECONDS)
      .build[UUID, Home[WorldId]]
      .asMap
      .asScala

  @Listener
  def onLogin(event: ServerSideConnectionEvent.Join): Unit =
    addToCache(event.getPlayer.getProfile)

  @Listener
  def onLogout(event: ServerSideConnectionEvent.Disconnect): Unit =
    val player = event.getPlayer
    removeFromCache(event.getPlayer.getProfile)
    requests.remove(player.getUniqueId)
    invites.remove(player.getUniqueId)

  //Get homes

  def allHomesForIdentInCache(identity: Identity): Map[String, Home[WorldId]] = 
    homeCache.getOrElse(identity.uuid, tempHomeCache.getOrElse(identity.uuid, Map.empty))
    
  def allHomesForIdent(identity: Identity): IO[Map[String, Home[WorldId]]] = { 
    IO(homeCache.get(identity.uuid).orElse(tempHomeCache.get(identity.uuid)))
      .flatMap(_.fold(allHomesForUUIDFromDb(identity.uuid))(IO.pure))
  }

  def getHomeInCache(identity: Identity, name: String): Option[Home[WorldId]] =
    homeCache.get(identity.uuid).orElse(tempHomeCache.get(identity.uuid)).flatMap(_.get(name))

  def getHome(identity: Identity, name: String): IO[Option[Home[WorldId]]] =
    IO(homeCache.get(identity.uuid).orElse(tempHomeCache.get(identity.uuid)))
      .flatMap(_.fold(getHomeFromDb(identity.uuid, name))(homes => IO.pure(homes.get(name))))

  def homeExistInCache(identity: Identity, name: String): Boolean =
    getHomeInCache(identity, name).isDefined

  def homeExist(identity: Identity, name: String): IO[Boolean] = 
    getHome(identity, name).map(_.isDefined)

  //Limits
  
  private def getLimit(subject: Subject, optionName: String, default: Int): Int =
    getOptionFromSubject(subject, optionName).flatMap(_.toIntOption).getOrElse(default)
  
  def getHomeLimit(subject: Subject): Int = 
    getLimit(subject, LibPerm.HomeLimitOption, HomeSweetHome.config.homeLimit)
  
  def getResidentLimit(subject: Subject): Int = 
    getLimit(subject, LibPerm.ResidentLimitOption, HomeSweetHome.config.residentLimit)
    
  //Requests
  
  def addRequest(requester: Identity, homeOwner: Identity, home: Home[WorldId]): Unit =
    requests.getOrElseUpdate(requester.uuid, createInvitesRequestsMap).put(homeOwner.uuid, home)

  def removeRequest(requester: Identity, homeOwner: Identity): Unit =
    requests.get(requester.uuid).foreach(_.remove(homeOwner.uuid))

  def getRequest(requester: Identity, homeOwner: Identity): Option[Home[WorldId]] =
    requests.get(requester.uuid).flatMap(_.get(homeOwner.uuid))
    
  //Invites

  def addInvite(target: Identity, homeOwner: Identity, home: Home[WorldId]): Unit =
    invites.getOrElseUpdate(target.uuid, createInvitesRequestsMap).put(homeOwner.uuid, home)

  def removeInvite(target: Identity, homeOwner: Identity): Unit =
    invites.get(target.uuid).foreach(_.remove(homeOwner.uuid))

  def isInvited(requester: Identity, homeOwner: Identity, home: Home[WorldId]): Boolean =
    invites.get(requester.uuid).flatMap(_.get(homeOwner.uuid)).contains(home)
    
  //Alter one home
  
  def updateHome(owner: Identity, name: String, home: Home[WorldId]): Unit =
    homeCache.updateWith(owner.uuid) {
      case Some(value) => Some(value.updated(name, home)) 
      case None =>        None
    }
    tempHomeCache.remove(owner.uuid)
    savePersistentHome(owner.uuid, name, home)

  def deleteHome(owner: Identity, name: String): Unit =
    homeCache.updateWith(owner.uuid) {
      case Some(value) => Some(value.removed(name))
      case None        => None
    }
    tempHomeCache.remove(owner.uuid)
    removePersistentHome(owner.uuid, name)
  
  //Cache handlers

  private def allHomesForUUIDFromDb(uuid: UUID): IO[Map[String, Home[WorldId]]] =
    sql"""|SELECT name,
          |       x,
          |       y,
          |       z,
          |       yaw,
          |       pitch,
          |       world_id,
          |       residents
          |    FROM homes
          |    WHERE owner = $uuid""".query[(String, Home[WorldId])].to[List].transact(xa).map(_.toMap)
  
  private def getHomeFromDb(uuid: UUID, name: String): IO[Option[Home[WorldId]]] = 
    sql"""|SELECT x, y, z, yaw, pitch, world_id, residents
          |    FROM homes
          |    WHERE owner = $uuid AND name = $name""".stripMargin.query[Home[WorldId]].option.transact(xa)

  private def addToCache(identity: Identity): Unit =
    allHomesForIdent(identity).unsafeToFuture().foreach(homes => homeCache.put(identity.uuid, homes))
  
  private def removeFromCache(identity: Identity): Unit =
    homeCache.remove(identity.uuid)
    tempHomeCache.remove(identity.uuid)
    
  def refreshCache(identities: Iterable[Identity]): Unit =
    identities
      .map(identity => allHomesForIdent(identity).map(identity.uuid -> _).unsafeToFuture())
      .foreach(_.foreach(homeCache.update(_, _)))
      
  def transferOldHomes(homes: Map[UUID, Map[String, OldHome]]): Unit =
    val homesFlat = homes.flatMap((uuid, playerHome) => playerHome.map((name, oldHome) => (uuid, name, oldHome)))
    val sqlStr = """|INSERT INTO homes (owner, name, x, y, z, yaw, pitch, world_id, residents)
                    |    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""".stripMargin
      
    Update[(UUID, String, Home[WorldId])](sqlStr).updateMany(??? : Vector[(UUID, String, Home[WorldId])]).transact(xa).unsafeRunSync()

  private def savePersistentHome(uuid: UUID, name: String, home: Home[WorldId]): Unit =
    sql"""|MERGE INTO homes (owner, name, x, y, z, yaw, pitch, world_id, residents) 
          |  KEY (owner, name) 
          |  VALUES ($uuid, $name, ${home.x}, ${home.y}, ${home.z}, ${home.yaw}, ${home.pitch}, ${home.worldId}, ${home.residents})"""
      .stripMargin.update.run.transact(xa).unsafeRunSync()

  private def removePersistentHome(uuid: UUID, name: String): Unit =
    sql"""DELETE FROM homes WHERE owner = $uuid AND name = $name""".update.run.transact(xa).unsafeRunSync()
