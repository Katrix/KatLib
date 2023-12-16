package net.katsstuff.magicalwarps

import com.github.benmanes.caffeine.cache.Caffeine

import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._
import cats.effect._

import java.util.Locale
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext

class WarpHandler[WorldId](using ExecutionContext) {
  
  private val warpsCache: mutable.Map[String, Warp[WorldId]] =
    Caffeine
      .newBuilder()
      .softValues()
      .expireAfterAccess(10, TimeUnit.MINUTES)
      .buildAsync[String, Warp[WorldId]] { (key, executor) =>
        getWarpFromDb(key).unsafeToFuture().map(_.orNull).asJava.toCompletableFuture
      }
      .synchronous
      .asMap
      .asScala

  private val groupsWarpNamesCache: mutable.Map[String, Seq[String]] =
    Caffeine
      .newBuilder()
      .softValues()
      .buildAsync[String, Seq[String]] { (key, executor) =>
        getGroupWarpNamesFromDb(key).unsafeToFuture().map(_.orNull).asJava.toCompletableFuture
      }
      .synchronous
      .asMap
      .asScala
      
  def getWarp(name: String): IO[Option[Warp[WorldId]]] = 
    IO(warpsCache.get(name)).flatMap(_.fold(getWarpFromDb(name))(w => IO.pure(Some(w))))

  def setWarp(name: String, warp: Warp[WorldId]): Unit = 
    ???
  
  def removeWarp(name: String): Unit = 
    warpsCache.remove(name)
    groupsWarpNamesCache.foreachEntry((k, v) => 
      if v.contains(name) then groupsWarpNamesCache.update(k, v.filter(_ != name))
    )
    ???

  def renameWarp(oldName: String, newName: String): Unit = 
    warpsCache.remove(oldName).foreach(warpsCache.update(newName, _))
    groupsWarpNamesCache.foreachEntry((k, v) => 
      if v.contains(oldName) then 
        groupsWarpNamesCache.update(k, v.map(s => if s == oldName then newName else s))
    )
    ???

  private def getWarpFromDb(warpName: String): IO[Option[Warp[WorldId]]] =
    ???
    
  private def getGroupWarpNamesFromDb(group: String): IO[Option[Seq[String]]] =
    ???

}
