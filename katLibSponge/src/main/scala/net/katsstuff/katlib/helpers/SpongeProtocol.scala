package net.katsstuff.katlib.helpers

import io.circe.Decoder.Result
import io.circe._
import io.circe.syntax._
import org.spongepowered.api.registry.RegistryEntry
import org.spongepowered.api.{ResourceKey, Sponge}

import java.net.{URI, URL}
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag
import scala.util.Try

trait SpongeProtocol:
  
  given Encoder[ResourceKey] with
    override def apply(a: ResourceKey): Json = Json.fromString(a.asString())
  
  given Decoder[ResourceKey] with
    override def apply(c: HCursor): Result[ResourceKey] = c.as[String].map(s => ResourceKey.resolve(s))
  
  /*
  given [A]: Encoder[RegistryEntry[A]] with
    override def apply(a: RegistryEntry[A]): Json = a.key.asJson

  given [A]: Decoder[RegistryEntry[A]] with
    override def apply(c: HCursor): Result[ResourceKey] = { 
      Sponge.getRegistry
      c.as[String].map(s => ResourceKey.resolve(s))
    }
   */
object SpongeProtocol extends SpongeProtocol