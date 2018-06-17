package net.katsstuff.katlib.impl

import java.util.UUID

import scala.collection.JavaConverters._

import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.player.{Player, User}
import org.spongepowered.api.service.user.UserStorageService

import cats.effect.Sync
import net.katstuff.katlib.algebras.{UserAccess, Users}
import net.katsstuff.katlib.helper.Implicits._

trait SpongeUsers[F[_], AbstractUser <: User] extends Users[F, AbstractUser, Player] {

  implicit def F: Sync[F]

  override def getPlayer(user: AbstractUser): F[Option[Player]] = F.delay(user.getPlayer.toOption)

  override def name(user: AbstractUser): F[String] = F.delay(user.getName)

  override def uniqueId(user: AbstractUser): UUID = user.getUniqueId

  override def hasPermission(user: AbstractUser, permission: String): F[Boolean] =
    F.delay(user.hasPermission(permission))

  override def getOption(user: AbstractUser, option: String): F[Option[String]] =
    F.delay(user.getOption(option).toOption)
}
class SpongeUsersClass[F[_]](implicit val F: Sync[F]) extends SpongeUsers[F, User]

class SpongeUserAccess[F[_]](implicit F: Sync[F]) extends UserAccess[F, User] {

  private def userStorage = Sponge.getServiceManager.provideUnchecked(classOf[UserStorageService])

  override def userByName(name: String): F[Option[User]] = F.delay(userStorage.get(name).toOption)

  override def userByUUID(uuid: UUID): F[Option[User]] = F.delay(userStorage.get(uuid).toOption)

  override def allUsers: F[Iterable[User]] = F.delay(userStorage.getAll.asScala.flatMap(userStorage.get(_).toOption))
}
