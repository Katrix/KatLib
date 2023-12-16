package net.katsstuff.magicalwarps

import net.katsstuff.minejson.text._

import java.util.UUID

case class Warp[WorldId](
    x: Double,
    y: Double,
    z: Double,
    yaw: Float,
    pitch: Float,
    worldId: WorldId,
    displayName: Option[Text],
    groups: Seq[String],
    allowedPermGroups: Seq[String],
    allowedUsers: Seq[UUID],
    lore: Option[Text],
    uses: Long
):

  def stringDisplayName(name: String): String = displayName.map(_.toPlain).getOrElse(name.capitalize)
  def textDisplayName(name: String): Text     = displayName.getOrElse(t"${name.capitalize}")
