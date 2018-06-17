package net.katsstuff.katlib.impl

import java.util.Locale

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

import cats.effect.Sync
import cats.syntax.all._
import net.katsstuff.katlib.helper.ProtocolHelper
import net.katsstuff.minejson.text.Text
import net.katstuff.katlib.algebras.CommandSources

trait BukkitCommandSources[F[_], Sender <: CommandSender] extends CommandSources[F, Sender] {
  implicit def F: Sync[F]

  override def name(source: Sender): F[String] = source.getName.pure

  override def hasPermission(source: Sender, permission: String): F[Boolean] =
    F.delay(source.hasPermission(permission))

  override def sendMessage(source: Sender, message: Text): F[Unit] = source match {
    case player: Player => F.delay(ProtocolHelper.sendPlayerMessage(player, message))
    case _              => F.delay(source.sendMessage(message.toPlain))
  }

  override def locale(source: Sender): F[Locale] = source match {
    case player: Player => F.catchNonFatal(Locale.forLanguageTag(player.getLocale))
    case _              => Locale.ROOT.pure
  }
}
class BukkitCommandSourcesClass[F[_]](implicit val F: Sync[F]) extends BukkitCommandSources[F, CommandSender]