package net.katsstuff.katlib.impl

import java.util.{Locale, UUID}

import scala.collection.JavaConverters._

import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.player.{Player, User}
import org.spongepowered.api.service.user.UserStorageService

import cats.effect.Sync
import cats.syntax.all._
import net.katsstuff.katlib.algebras.TextConversion
import net.katsstuff.minejson.text.Text
import net.katstuff.katlib.algebras.PlayerAccess
import net.katsstuff.katlib.helper.Implicits._

class SpongePlayerAccess[F[_]](implicit F: Sync[F], T: TextConversion[F]) extends PlayerAccess[F, Player, User] {

  private def server = Sponge.getServer

  private def userStorage = Sponge.getServiceManager.provideUnchecked(classOf[UserStorageService])

  override def playerByName(name: String): F[Option[Player]] = F.delay(server.getPlayer(name).toOption)

  override def userByName(name: String): F[Option[User]] = F.delay(userStorage.get(name).toOption)

  override def playerByUUID(uuid: UUID): F[Option[Player]] = F.delay(server.getPlayer(uuid).toOption)

  override def userByUUID(uuid: UUID): F[Option[User]] = F.delay(userStorage.get(uuid).toOption)

  override def onlinePlayers: F[Set[Player]] = F.delay(server.getOnlinePlayers.asScala.toSet)

  override def allUsers: F[Iterable[User]] = F.delay(userStorage.getAll.asScala.flatMap(userStorage.get(_).toOption))

  override def sendMessage(player: Player, message: Text): F[Unit] =
    T.ourToSponge(message).flatMap(txt => F.delay(player.sendMessage(txt)))

  override def name(player: Player): F[String] = player.getName.pure

  override def displayName(player: Player): F[Text] =
    F.delay(player.getDisplayNameData.displayName.get).flatMap(T.spongeToOur)

  override def locale(player: Player): F[Locale] = player.getLocale.pure
}
