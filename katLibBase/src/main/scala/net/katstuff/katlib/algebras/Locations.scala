package net.katstuff.katlib.algebras

import java.util.UUID

trait Locations[F[_], Location, Locateable] {

  /**
    * Get the x part of this location.
    */
  def getX(location: Location): F[Double]

  /**
    * Get the y part of this location.
    */
  def getY(location: Location): F[Double]

  /**
    * Get the z part of this location.
    */
  def getZ(location: Location): F[Double]

  /**
    * Get the yaw part of this location.
    */
  def getYaw(location: Location): F[Double]

  /**
    * Get the pitch part of this location.
    */
  def getPitch(location: Location): F[Double]

  /**
    * Get the world id part of this location.
    */
  def getWorldId(location: Location): F[UUID]

  /**
    * Get the current location of a Locateable
    */
  def getLocation(locateable: Locateable): F[Location]

  /**
    * Get the name of a world.
    */
  def getWorldName(worldId: UUID): F[Option[String]]

  /**
    * Try to find a safe location near an existing location.
    */
  def getSafeLocation(location: Location): F[Option[Location]]

}
