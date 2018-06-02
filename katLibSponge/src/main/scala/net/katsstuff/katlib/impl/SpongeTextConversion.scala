package net.katsstuff.katlib.impl

import org.spongepowered.api.text.serializer.TextSerializers
import org.spongepowered.api.text.{Text => SpongeText}

import cats.MonadError
import cats.syntax.applicative._
import net.katsstuff.katlib.algebras.TextConversion
import net.katsstuff.minejson.text.Text
import net.katsstuff.minejson.text.serializer.JsonTextSerializer

//I'm lazy and rely on json
class SpongeTextConversion[F[_]](implicit F: MonadError[F, Throwable]) extends TextConversion[F] {

  override def ourToSponge(our: Text): F[SpongeText] =
    TextSerializers.JSON.deserialize(our.toJson).pure

  override def spongeToOur(sponge: SpongeText): F[Text] =
    F.fromTry(JsonTextSerializer.deserialize(TextSerializers.JSON.serialize(sponge)))
}
