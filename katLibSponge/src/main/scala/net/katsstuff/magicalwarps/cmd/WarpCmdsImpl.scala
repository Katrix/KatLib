package net.katsstuff.magicalwarps.cmd

import net.katsstuff.magicalwarps.{Warp, WarpHandler}
import net.katsstuff.minejson.text._
import net.katsstuff.katlib.helpers._
import net.kyori.adventure.audience.Audience
import org.spongepowered.api.command.CommandResults

trait WarpCmdsImpl[
  CommandResult, 
  Location, 
  Rotation, 
  Teleportable, 
  WorldId
](using warpHandler: WarpHandler[WorldId]):

  val CmdSuccess: CommandResult

  def makeWarp(location: Location, rotation: Rotation): Warp[WorldId]

  def setWarp(audience: Audience, warpName: String, groups: Seq[String], location: Location, rotation: Rotation) =
    warpHandler.setWarp(warpName, makeWarp(location, rotation).copy(groups = groups))
    audience.sendMessage(t"${Green}Set warp $Aqua$warpName")
    CmdSuccess
  
