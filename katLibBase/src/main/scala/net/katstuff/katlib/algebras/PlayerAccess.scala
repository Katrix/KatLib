package net.katstuff.katlib.algebras

import java.util.{Locale, UUID}

import net.katsstuff.minejson.text.Text

/**
  * Provides access to some properties and actions to execute on a player,
  * and a way to acquire players.
  */
trait PlayerAccess[F[_], Player, User] {

  /**
    * Gets a player by name.
    */
  def playerByName(name: String): F[Option[Player]]

  /**
    * Gets a user by name.
    */
  def userByName(name: String): F[Option[User]]

  /**
    * Gets a player by [[UUID]].
    */
  def playerByUUID(uuid: UUID): F[Option[Player]]

  /**
    * Gets a player by [[UUID]].
    */
  def userByUUID(uuid: UUID): F[Option[User]]

  /**
    * Gets all the online players.
    */
  def onlinePlayers: F[Set[Player]]

  /**
    * Gets all users that have visited this server. Warning, might be a bit slow.
    */
  def allUsers: F[Iterable[User]]

  /**
    * Sends a message to a player.
    */
  def sendMessage(player: Player, message: Text): F[Unit]

  /**
    * Gets the name of a player.
    */
  def name(player: Player): F[String]

  /**
    * Gets the display name of a player.
    */
  def displayName(player: Player): F[Text]

  /**
    * Gets the locale of a player.
    */
  def locale(player: Player): F[Locale]

}
