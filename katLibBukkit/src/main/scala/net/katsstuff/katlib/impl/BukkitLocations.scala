package net.katsstuff.katlib.impl

import java.util.UUID

import org.bukkit.{Bukkit, Location}
import org.bukkit.entity.Player

import cats.effect.Sync
import net.katsstuff.katlib.algebras.Locations

class BukkitLocations[F[_]](implicit F: Sync[F]) extends Locations[F, Location, Player] {
  override def getX(location: Location):       F[Double] = F.delay(location.getX)
  override def getY(location: Location):       F[Double] = F.delay(location.getY)
  override def getZ(location: Location):       F[Double] = F.delay(location.getZ)
  override def getYaw(location: Location):     F[Double] = F.delay(location.getPitch)
  override def getPitch(location: Location):   F[Double] = F.delay(location.getYaw)
  override def getWorldId(location: Location): F[UUID]   = F.delay(location.getWorld.getUID)

  override def getLocation(locateable: Player):     F[Location]         = F.delay(locateable.getLocation)
  override def getWorldName(worldId: UUID):         F[Option[String]]   = F.delay(Option(Bukkit.getWorld(worldId)).map(_.getName))
  override def getSafeLocation(location: Location): F[Option[Location]] = F.pure(Some(location))
}
