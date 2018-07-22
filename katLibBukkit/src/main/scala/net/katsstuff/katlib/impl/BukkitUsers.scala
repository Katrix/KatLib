package net.katsstuff.katlib.impl

import java.util.UUID

import org.bukkit.entity.Player
import org.bukkit.{Bukkit, OfflinePlayer}

import cats.effect.Sync
import cats.syntax.all._
import net.katsstuff.katlib.algebras.{UserAccess, Users}
import net.milkbowl.vault.chat.Chat
import net.milkbowl.vault.permission.Permission

trait BukkitUsers[F[_], User <: OfflinePlayer] extends Users[F, User, Player] {

  private val chat        = Bukkit.getServicesManager.load(classOf[Chat])
  private val permissions = Bukkit.getServicesManager.load(classOf[Permission])

  implicit def F: Sync[F]

  override def getPlayer(user: User): F[Option[Player]] = F.delay(Option(user.getPlayer))

  override def name(user: User): F[String] = F.delay(user.getName)

  override def uniqueId(user: User): UUID = user.getUniqueId
  override def hasPermission(user: User, permission: String): F[Boolean] =
    F.delay(permissions.playerHas(null, user, permission))
  override def getOption(user: User, option: String): F[Option[String]] =
    F.delay(Some(chat.getPlayerInfoString(Bukkit.getWorlds.get(0).getName, user, option, "")).filter(_.nonEmpty))
}
class BukkitUsersClass[F[_]](implicit val F: Sync[F]) extends BukkitUsers[F, OfflinePlayer]

class BukkitUserAccess[F[_]](implicit F: Sync[F]) extends UserAccess[F, OfflinePlayer] {
  override def userByName(name: String): F[Option[OfflinePlayer]] = allUsers.map(_.find(_.getName == name))

  override def userByUUID(uuid: UUID): F[Option[OfflinePlayer]] = allUsers.map(_.find(_.getUniqueId == uuid))

  override def allUsers: F[Iterable[OfflinePlayer]] = F.delay(Bukkit.getOfflinePlayers)
}
