package net.katsstuff.homesweethome

import java.util.UUID

case class Home[WorldId](
  x: Double,
  y: Double,
  z: Double,
  yaw: Double,
  pitch: Double,
  worldId: WorldId,
  residents: Vector[UUID]
):

  def addResident(resident: UUID): Home[WorldId] = copy(residents = (residents :+ resident).distinct)

  def removeResident(resident: UUID): Home[WorldId] = copy(residents = residents.filter(_ != resident))
