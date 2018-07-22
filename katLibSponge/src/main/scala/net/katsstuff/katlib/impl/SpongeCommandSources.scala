package net.katsstuff.katlib.impl

import java.util.Locale

import org.spongepowered.api.command.CommandSource

import cats.effect.Sync
import cats.syntax.all._
import net.katsstuff.katlib.algebras.{CommandSources, TextConversion}
import net.katsstuff.minejson.text.Text

trait SpongeCommandSources[F[_], Source <: CommandSource] extends CommandSources[F, Source] {

  implicit def F: Sync[F]
  implicit def T: TextConversion[F]

  override def name(source: Source): F[String] =
    source.getName.pure

  override def hasPermission(source: Source, permission: String): F[Boolean] =
    F.delay(source.hasPermission(permission))

  override def sendMessage(source: Source, message: Text): F[Unit] =
    T.ourToSponge(message).flatMap(txt => F.delay(source.sendMessage(txt)))

  override def locale(source: Source): F[Locale] =
    source.getLocale.pure
}
class SpongeCommandSourcesClass[F[_]](implicit val F: Sync[F], val T: TextConversion[F])
    extends SpongeCommandSources[F, CommandSource]
