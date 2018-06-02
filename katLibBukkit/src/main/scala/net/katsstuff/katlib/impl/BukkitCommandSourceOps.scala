package net.katsstuff.katlib.impl

import java.util.Locale

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

import cats.effect.Sync
import cats.syntax.all._
import net.katsstuff.katlib.helper.ProtocolHelper
import net.katsstuff.minejson.text.Text
import net.katstuff.katlib.algebras.CommandSourceAccess

class BukkitCommandSourceOps[F[_]](implicit F: Sync[F]) extends CommandSourceAccess[F, CommandSender] {
  override def name(source: CommandSender): F[String] = source.getName.pure

  override def hasPermission(source: CommandSender, permission: String): F[Boolean] =
    F.delay(source.hasPermission(permission))

  override def sendMessage(source: CommandSender, message: Text): F[Unit] = source match {
    case player: Player => F.delay(ProtocolHelper.sendPlayerMessage(player, message))
    case _              => F.delay(source.sendMessage(message.toPlain))
  }

  override def locale(source: CommandSender): F[Locale] = source match {
    case player: Player => F.catchNonFatal(Locale.forLanguageTag(player.getLocale))
    case _              => Locale.ROOT.pure
  }
}
