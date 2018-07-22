package net.katsstuff.katlib.impl

import java.util.UUID

import scala.collection.JavaConverters._

import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.player.Player

import cats.effect.Sync
import cats.syntax.all._
import net.katsstuff.katlib.algebras.{Players, TextConversion}
import net.katsstuff.katlib.helper.Implicits._
import net.katsstuff.minejson.text.Text

class SpongePlayers[F[_]](implicit val F: Sync[F], val T: TextConversion[F])
    extends Players[F, Player]
    with SpongeCommandSources[F, Player]
    with SpongeUsers[F, Player] {

  private def server = Sponge.getServer

  override def name(user: Player): F[String] = super[SpongeUsers].name(user)

  override def hasPermission(user: Player, permission: String): F[Boolean] = super[SpongeUsers].hasPermission(user, permission)

  override def playerByName(name: String): F[Option[Player]] = F.delay(server.getPlayer(name).toOption)

  override def playerByUUID(uuid: UUID): F[Option[Player]] = F.delay(server.getPlayer(uuid).toOption)

  override def onlinePlayers: F[Set[Player]] = F.delay(server.getOnlinePlayers.asScala.toSet)

  override def displayName(player: Player): F[Text] =
    F.delay(player.getDisplayNameData.displayName.get).flatMap(T.spongeToOur)
}
