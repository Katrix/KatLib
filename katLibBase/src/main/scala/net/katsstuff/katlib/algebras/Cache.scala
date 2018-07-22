package net.katsstuff.katlib.algebras

import scala.concurrent.duration.Duration

/**
  * Provides a means to create a cache, and operate on it.
  */
trait Cache[F[_]] {
  type CacheType[K, V]

  /**
    * Creates a cache where entries expire after being written.
    * @param duration How long a key should last after being written.
    */
  def createExpireAfterWrite[K, V](duration: Duration): CacheType[K, V]

  /**
    * Gets a value from a cache.
    */
  def get[K, V](cache: CacheType[K, V])(key: K): F[Option[V]]

  /**
    * Put a value in a cache.
    */
  def put[K, V](cache: CacheType[K, V])(key: K, value: V): F[Unit]
}
