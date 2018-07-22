package net.katsstuff.katlib.algebras

import java.util.UUID

import net.katsstuff.minejson.text.Text

/**
  * Provides access to some properties and actions to execute on a player,
  * and a way to acquire players.
  */
trait Players[F[_], Player] extends Users[F, Player, Player] with CommandSources[F, Player] {

  /**
    * Gets a player by name.
    */
  def playerByName(name: String): F[Option[Player]]

  /**
    * Gets a player by [[UUID]].
    */
  def playerByUUID(uuid: UUID): F[Option[Player]]

  /**
    * Gets all the online players.
    */
  def onlinePlayers: F[Set[Player]]

  /**
    * Gets the display name of a player.
    */
  def displayName(player: Player): F[Text]

}
