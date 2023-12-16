package net.katsstuff.homesweethome.cmd

import net.katsstuff.homesweethome.Home
import net.kyori.adventure.identity.Identity
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.entity.living.player.server.ServerPlayer

case class GenHomeWithName[WorldId](name: String, home: Home[WorldId])
case class GenOtherHome[WorldId](isOther: Boolean, namedHome: GenHomeWithName[WorldId], homeOwner: Identity):
  def home: Home[WorldId] = namedHome.home

object GenOtherHome:
  def same[WorldId](home: GenHomeWithName[WorldId], identity: Identity): GenOtherHome[WorldId] = GenOtherHome(isOther = false, home, identity)
