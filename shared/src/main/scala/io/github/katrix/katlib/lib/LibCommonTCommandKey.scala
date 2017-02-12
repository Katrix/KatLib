package io.github.katrix.katlib.lib

import org.spongepowered.api.entity.living.player.{Player, User}
import org.spongepowered.api.world.storage.WorldProperties
import org.spongepowered.api.world.{DimensionType, Location, World}

import io.github.katrix.katlib.helper.Implicits._

object LibCommonTCommandKey {

	val Command  : CommandKey[String]          = cmdKey[String](t"command")
	val Player   : CommandKey[Player]          = cmdKey[Player](t"player")
	val User     : CommandKey[User]            = cmdKey[User](t"user")
	val World    : CommandKey[WorldProperties] = cmdKey[WorldProperties](t"world")
	val Dimension: CommandKey[DimensionType]   = cmdKey[DimensionType](t"dimension")
	val Location : CommandKey[Location[World]] = cmdKey[Location[World]](t"location")
	val String   : CommandKey[String]          = cmdKey[String](t"string")
	val Int      : CommandKey[Int]             = cmdKey[Int](t"int")
	val Double   : CommandKey[Double]          = cmdKey[Double](t"")
	val Boolean  : CommandKey[Boolean]         = cmdKey[Boolean](t"boolean")

}
