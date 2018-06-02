package net.katsstuff.katlib.impl

import java.util.logging.Level

import cats.effect.Sync
import net.katsstuff.katlib.ScalaPluginIO
import net.katstuff.katlib.algebras.LogHelper

class BukkitLogHelper[F[_]](implicit F: Sync[F], scalaPlugin: ScalaPluginIO) extends LogHelper[F] {
  override def trace(any: Any): F[Unit] = F.delay(scalaPlugin.logger.finer(any.toString))

  override def debug(any: Any): F[Unit] = F.delay(scalaPlugin.logger.fine(any.toString))

  override def info(any: Any): F[Unit] = F.delay(scalaPlugin.logger.info(any.toString))

  override def warn(any: Any):                  F[Unit] = F.delay(scalaPlugin.logger.warning(any.toString))
  override def warn(msg: String, e: Throwable): F[Unit] = F.delay(scalaPlugin.logger.log(Level.WARNING, msg, e))

  override def error(any: Any):                  F[Unit] = F.delay(scalaPlugin.logger.severe(any.toString))
  override def error(msg: String, e: Throwable): F[Unit] = F.delay(scalaPlugin.logger.log(Level.SEVERE, msg, e))
}
