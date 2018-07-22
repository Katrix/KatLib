package net.katsstuff.katlib.impl

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration.Duration

import com.google.common.cache.CacheBuilder

import cats.effect.Sync
import net.katsstuff.katlib.algebras.Cache

class SpongeCache[F[_]](implicit F: Sync[F]) extends Cache[F] {
  override type CacheType[K, V] = mutable.Map[K, V]
  override def createExpireAfterWrite[K, V](duration: Duration): CacheType[K, V] =
    CacheBuilder
      .newBuilder()
      .expireAfterWrite(duration.length, duration.unit)
      .build[AnyRef, AnyRef]
      .asMap
      .asScala
      .asInstanceOf[CacheType[K, V]] //Fixes object upper bound error

  override def get[K, V](cache: CacheType[K, V])(key: K):           F[Option[V]] = F.delay(cache.get(key))
  override def put[K, V](cache: CacheType[K, V])(key: K, value: V): F[Unit]      = F.delay(cache.put(key, value))
}
