package net.katsstuff.katlib.impl

import java.util.Locale

import org.spongepowered.api.command.CommandSource

import cats.effect.Sync
import cats.syntax.all._
import net.katsstuff.katlib.algebras.TextConversion
import net.katsstuff.minejson.text.Text
import net.katstuff.katlib.algebras.CommandSourceAccess

class SpongeCommandSourceOps[F[_]](implicit F: Sync[F], T: TextConversion[F]) extends CommandSourceAccess[F, CommandSource] {

  override def name(source: CommandSource): F[String] =
    source.getName.pure

  override def hasPermission(source: CommandSource, permission: String): F[Boolean] =
    F.delay(source.hasPermission(permission))

  override def sendMessage(source: CommandSource, message: Text): F[Unit] =
    T.ourToSponge(message).flatMap(txt => F.delay(source.sendMessage(txt)))

  override def locale(source: CommandSource): F[Locale] =
    source.getLocale.pure
}
