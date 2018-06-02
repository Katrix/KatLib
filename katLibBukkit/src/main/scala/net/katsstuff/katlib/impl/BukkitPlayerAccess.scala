package net.katsstuff.katlib.impl

import java.util.{Locale, UUID}

import scala.collection.JavaConverters._

import org.bukkit.{Bukkit, OfflinePlayer}
import org.bukkit.entity.Player

import cats.effect.Sync
import cats.syntax.all._
import net.katsstuff.katlib.helper.ProtocolHelper
import net.katsstuff.minejson.text.Text
import net.katsstuff.minejson.text.serializer.FormattingCodeSerializer
import net.katstuff.katlib.algebras.PlayerAccess

class BukkitPlayerAccess[F[_]](implicit F: Sync[F]) extends PlayerAccess[F, Player, OfflinePlayer] {

  override def playerByName(name: String): F[Option[Player]] = F.delay(Option(Bukkit.getPlayer(name)))

  override def userByName(name: String): F[Option[OfflinePlayer]] = allUsers.map(_.find(_.getName == name))

  override def playerByUUID(uuid: UUID): F[Option[Player]] = F.delay(Option(Bukkit.getPlayer(uuid)))

  override def userByUUID(uuid: UUID): F[Option[OfflinePlayer]] = allUsers.map(_.find(_.getUniqueId == uuid))

  override def onlinePlayers: F[Set[Player]] = F.delay(Bukkit.getOnlinePlayers.asScala.toSet)

  override def allUsers: F[Iterable[OfflinePlayer]] = F.delay(Bukkit.getOfflinePlayers)

  override def sendMessage(player: Player, message: Text): F[Unit] =
    F.delay(ProtocolHelper.sendPlayerMessage(player, message))

  override def name(player: Player): F[String] = player.getName.pure

  override def displayName(player: Player): F[Text] =
    F.suspend(F.fromTry(FormattingCodeSerializer.deserialize(player.getDisplayName)))

  override def locale(player: Player): F[Locale] = F.catchNonFatal(Locale.forLanguageTag(player.getLocale))
}
