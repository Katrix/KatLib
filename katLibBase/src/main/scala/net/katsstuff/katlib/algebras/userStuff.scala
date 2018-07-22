package net.katsstuff.katlib.algebras

import java.util.UUID

trait Users[F[_], User, Player] {

  /**
    * Get the player for a user if they are online.
    */
  def getPlayer(user: User): F[Option[Player]]

  /**
    * Gets the name of a user.
    */
  def name(user: User): F[String]

  /**
    * Gets the UUID of a user.
    */
  def uniqueId(user: User): UUID

  /**
    * Checks if a command source has a permission.
    */
  def hasPermission(user: User, permission: String): F[Boolean]

  /**
    * Get the permission option for a user.
    */
  def getOption(user: User, option: String): F[Option[String]]

}

trait UserAccess[F[_], User] {

  /**
    * Gets a user by name.
    */
  def userByName(name: String): F[Option[User]]

  /**
    * Gets a player by [[UUID]].
    */
  def userByUUID(uuid: UUID): F[Option[User]]

  /**
    * Gets all users that have visited this server. Warning, might be a bit slow.
    */
  def allUsers: F[Iterable[User]]
}
