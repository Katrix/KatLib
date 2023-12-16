package net.katsstuff.katlib.spongecommands

import org.spongepowered.api.command.parameter.CommandContext

sealed trait ParentParams[R <: Tuple]:
  def getValuesFromContext(ctx: CommandContext): R
  def simplify: ParentParams[R]

object ParentParams:
  private[spongecommands] case class ParentParamsImpl[R <: Tuple, C <: Tuple, F <: Tuple, P <: Tuple](
    parent: ParentParams[R],
    contextExtractor: KatCauseExtractors[C],
    flags: KatFlags[F],
    parameters: KatParameters[P]
  ) extends ParentParams[CommandTuple[R, C, F, P]]:
    override def getValuesFromContext(ctx: CommandContext): CommandTuple[R, C, F, P] =
      val r = parent.getValuesFromContext(ctx)
      val c = contextExtractor.getValuesFromContext(ctx)
      val f = flags.getValuesFromContext(ctx)
      val p = parameters.getValuesFromContext(ctx)
      (r ++ (c ++ (f ++ p)))

    override def simplify: ParentParams[CommandTuple[R, C, F, P]] =
      ParentParamsImpl(parent.simplify, contextExtractor.simplify, flags.simplify, parameters.simplify)

  private[spongecommands] case object NoParentParams extends ParentParams[EmptyTuple]:
    override def getValuesFromContext(ctx: CommandContext): EmptyTuple = EmptyTuple

    override def simplify: ParentParams[EmptyTuple] = this
end ParentParams
