package net.katsstuff.katlib.scsponge70

import org.spongepowered.api.event.cause.{Cause => SpongeCause}

object Cause {

  def builder: SpongeCause.Builder = SpongeCause.builder()

  def of(ctx: EventContext, cause: AnyRef): Cause = SpongeCause.of(ctx, cause)

  def of(ctx: EventContext, cause: AnyRef, causes: AnyRef*): Cause = SpongeCause.of(ctx, cause, causes: _*)

  def of(ctx: EventContext, iterable: Iterable[AnyRef]): Cause = SpongeCause.of(ctx, iterable)
}
