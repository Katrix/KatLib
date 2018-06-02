package net.katsstuff.katlib.impl

import cats.effect.Sync
import net.katsstuff.katlib.KatPlugin
import net.katstuff.katlib.algebras.LogHelper

class SpongeLogHelper[F[_]](implicit plugin: KatPlugin, F: Sync[F]) extends LogHelper[F] {

  override def trace(any: Any): F[Unit] = F.delay(plugin.logger.trace(any.toString))

  override def debug(any: Any): F[Unit] = F.delay(plugin.logger.debug(any.toString))

  override def info(any: Any): F[Unit] = F.delay(plugin.logger.info(any.toString))

  override def warn(any: Any):                  F[Unit] = F.delay(plugin.logger.warn(any.toString))
  override def warn(msg: String, e: Throwable): F[Unit] = F.delay(plugin.logger.warn(msg, e))

  override def error(any: Any):                  F[Unit] = F.delay(plugin.logger.error(any.toString))
  override def error(msg: String, e: Throwable): F[Unit] = F.delay(plugin.logger.error(msg, e))
}
