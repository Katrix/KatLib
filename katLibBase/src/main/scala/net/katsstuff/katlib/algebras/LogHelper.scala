package net.katsstuff.katlib.algebras

/**
  * Provides access to a way to do logging.
  */
trait LogHelper[F[_]] {

  def trace(any: Any): F[Unit]

  def debug(any: Any): F[Unit]

  def info(any: Any): F[Unit]

  def warn(any: Any): F[Unit]

  def warn(msg: String, e: Throwable): F[Unit]

  def error(any: Any): F[Unit]

  def error(msg: String, e: Throwable): F[Unit]
}
