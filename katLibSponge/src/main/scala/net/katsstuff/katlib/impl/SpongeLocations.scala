package net.katsstuff.katlib.impl

import java.util.UUID

import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.world.{Location, World}

import cats.effect.Sync
import cats.syntax.all._
import net.katsstuff.katlib.helper.Implicits._
import net.katstuff.katlib.algebras.Locations

class SpongeLocations[F[_]](implicit F: Sync[F]) extends Locations[F, SpongeLocation, Player] {
  override def getX(location: SpongeLocation):       F[Double] = location.x.pure
  override def getY(location: SpongeLocation):       F[Double] = location.y.pure
  override def getZ(location: SpongeLocation):       F[Double] = location.z.pure
  override def getYaw(location: SpongeLocation):     F[Double] = location.yaw.pure
  override def getPitch(location: SpongeLocation):   F[Double] = location.pitch.pure
  override def getWorldId(location: SpongeLocation): F[UUID]   = location.worldId.pure

  override def getLocation(locateable: Player): F[SpongeLocation] =
    F.delay(SpongeLocation(locateable.getLocation, locateable.getRotation.getX, locateable.getRotation.getY))
  override def getWorldName(worldId: UUID): F[Option[String]] =
    F.delay(Sponge.getServer.getWorld(worldId).toOption.map(_.getName))
  override def getSafeLocation(location: SpongeLocation): F[Option[SpongeLocation]] =
    F.delay(
      Sponge.getGame.getTeleportHelper.getSafeLocation(location.location).toOption.map(loc => location.copy(location = loc))
    )
}

case class SpongeLocation(location: Location[World], yaw: Double, pitch: Double) {
  def x:       Double = location.getX
  def y:       Double = location.getY
  def z:       Double = location.getZ
  def worldId: UUID   = location.getExtent.getUniqueId
}
