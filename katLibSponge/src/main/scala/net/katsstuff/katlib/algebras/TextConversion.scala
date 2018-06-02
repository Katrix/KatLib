package net.katsstuff.katlib.algebras

import org.spongepowered.api.text.{Text => SpongeText}

import net.katsstuff.minejson.text._

/**
  * An algebra for converting our text classes to the sponge ones and back.
  */
trait TextConversion[F[_]] {

  /**
    * Convert our text class to sponge.
    */
  def ourToSponge(our: Text): F[SpongeText]

  /**
    * Convert Sponge's text classes to ours.
    */
  def spongeToOur(sponge: SpongeText): F[Text]

}
