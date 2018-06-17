package net.katsstuff.katlib.impl

import java.util.{Locale, UUID}

import scala.collection.JavaConverters._

import org.bukkit.Bukkit
import org.bukkit.entity.Player

import cats.effect.Sync
import net.katsstuff.katlib.helper.ProtocolHelper
import net.katsstuff.minejson.text.Text
import net.katsstuff.minejson.text.serializer.FormattingCodeSerializer
import net.katstuff.katlib.algebras.Players
import net.milkbowl.vault.chat.Chat
import net.milkbowl.vault.permission.Permission

class BukkitPlayers[F[_]](implicit val F: Sync[F])
    extends Players[F, Player]
    with BukkitCommandSources[F, Player]
    with BukkitUsers[F, Player] {

  private val chat        = Bukkit.getServicesManager.load(classOf[Chat])
  private val permissions = Bukkit.getServicesManager.load(classOf[Permission])

  override def playerByName(name: String): F[Option[Player]] = F.delay(Option(Bukkit.getPlayer(name)))

  override def playerByUUID(uuid: UUID): F[Option[Player]] = F.delay(Option(Bukkit.getPlayer(uuid)))

  override def onlinePlayers: F[Set[Player]] = F.delay(Bukkit.getOnlinePlayers.asScala.toSet)

  override def sendMessage(player: Player, message: Text): F[Unit] =
    F.delay(ProtocolHelper.sendPlayerMessage(player, message))

  override def name(player: Player): F[String] = F.delay(player.getName)

  override def uniqueId(player: Player): UUID = player.getUniqueId

  override def displayName(player: Player): F[Text] =
    F.suspend(F.fromTry(FormattingCodeSerializer.deserialize(player.getDisplayName)))

  override def locale(player: Player): F[Locale] = F.catchNonFatal(Locale.forLanguageTag(player.getLocale))

  override def hasPermission(user: Player, permission: String): F[Boolean] =
    F.delay(permissions.playerHas(user, permission))

  override def getOption(user: Player, option: String): F[Option[String]] =
    F.delay(Some(chat.getPlayerInfoString(user, option, "")).filter(_.nonEmpty))
}
