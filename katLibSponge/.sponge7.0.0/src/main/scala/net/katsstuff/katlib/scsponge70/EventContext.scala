package net.katsstuff.katlib.scsponge70

import scala.collection.JavaConverters._

import org.spongepowered.api.event.cause.{EventContext => SpongeEventContext}

object EventContext {

  def apply(entries: Map[EventContextKey[_], AnyRef]): EventContext = SpongeEventContext.of(entries.asJava)

}
