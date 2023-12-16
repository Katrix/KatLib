package net.katsstuff.katlib.spongecommands

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.identity.Identity
import org.spongepowered.api.block.BlockSnapshot
import org.spongepowered.api.command.parameter.CommandContext
import org.spongepowered.api.entity.Entity
import org.spongepowered.api.entity.living.player.server.ServerPlayer
import org.spongepowered.api.service.permission.Subject
import org.spongepowered.api.util.Nameable
import org.spongepowered.api.world.server.ServerLocation
import org.spongepowered.math.vector.Vector3d
import perspective._
import perspective.derivation._

sealed trait KatCauseExtractors[C <: Tuple]:
  def ~[C2 <: Tuple](that: KatCauseExtractors[C2]): KatCauseExtractors[Tuple.Concat[C, C2]] =
    KatCauseExtractors.ExtractorConcat(this, that)

  def getValuesFromContext(context: CommandContext): C

  def simplify: KatCauseExtractors[C]

  def map[B](f: C => B): KatCauseExtractors[Tuple1[B]]
object KatCauseExtractors:
  private[spongecommands] case object Empty extends KatCauseExtractors[EmptyTuple]:
    override def getValuesFromContext(context: CommandContext): EmptyTuple = EmptyTuple

    override def simplify: KatCauseExtractors[EmptyTuple] = this

    override def map[B](f: EmptyTuple => B): KatCauseExtractors[Tuple1[B]] =
      MappedExtractors(this, f)

  private[spongecommands] case class ExtractorConcat[C1 <: Tuple, C2 <: Tuple](
    c1: KatCauseExtractors[C1],
    c2: KatCauseExtractors[C2]
  ) extends KatCauseExtractors[Tuple.Concat[C1, C2]]:
    override def getValuesFromContext(context: CommandContext): Tuple.Concat[C1, C2] =
      c1.getValuesFromContext(context) ++ c2.getValuesFromContext(context)

    override def simplify: KatCauseExtractors[Tuple.Concat[C1, C2]] =
      val simplifyResult = (c1.simplify, c2.simplify) match
        case (Empty, s2) => Left(s2.asInstanceOf[KatCauseExtractors[Tuple.Concat[C1, C2]]])
        case (s1, Empty) => Left(s1.asInstanceOf[KatCauseExtractors[Tuple.Concat[C1, C2]]])

        case (ExtractorProductK(s1), ExtractorProductK(s2)) =>
          Right((s1.tuple ++ s2.tuple))

        case (ExtractorProductK(s1), s2: KatCauseExtractor[_]) =>
          Right((s1.tuple ++ Tuple1(s2)))

        case (s1: KatCauseExtractor[_], ExtractorProductK(s2))  =>
          Right((s1 *: s2.tuple))

        case (s1: KatCauseExtractor[_], s2: KatCauseExtractor[_]) =>
          Right((s1, s2))

        case (s1, s2) => Left(ExtractorConcat(s1, s2))

      simplifyResult match
        case Left(res)    => res
        case Right(value) =>
          val tuples = value.asInstanceOf[Tuple.Map[Tuple.Concat[C1, C2], KatCauseExtractor]]
          val size = tuples.size.asInstanceOf[Tuple.Size[Tuple.Concat[C1, C2]]]
          ExtractorProductK(ProductK.of(tuples))(using ValueOf(size))

    override def map[B](f: Tuple.Concat[C1, C2] => B): KatCauseExtractors[Tuple1[B]] =
      MappedExtractors(this, f)

  private[spongecommands] case class MappedExtractors[A <: Tuple, B](orig: KatCauseExtractors[A], f: A => B) extends KatCauseExtractors[Tuple1[B]]:
    override def getValuesFromContext(context: CommandContext): Tuple1[B] =
      Tuple1(f(orig.getValuesFromContext(context)))

    override def simplify: KatCauseExtractors[Tuple1[B]] = copy(orig = orig.simplify)

    override def map[C](f: Tuple1[B] => C): KatCauseExtractors[Tuple1[C]] =
      copy(f = this.f.andThen(b => f(Tuple1(b))))

  private[spongecommands] case class ExtractorProductK[C <: Tuple](
    extractors: ProductK[KatCauseExtractor, C]
  )(using ValueOf[Tuple.Size[C]]) extends KatCauseExtractors[C]:
    private inline def instance = ProductK.productKInstance[C]

    override def getValuesFromContext(context: CommandContext): C =
      instance
        .mapK[KatCauseExtractor, cats.Id, Nothing](extractors)([Z] => (extractor: KatCauseExtractor[Z]) => extractor.getValuesFromContext(context)._1)
    .tuple
      .asInstanceOf[C]

    override def simplify: KatCauseExtractors[C] = this

    override def map[B](f: C => B): KatCauseExtractors[Tuple1[B]] = MappedExtractors(this, f)
end KatCauseExtractors

trait KatCauseExtractor[A] extends KatCauseExtractors[Tuple1[A]]
object KatCauseExtractor:
  val audience: KatCauseExtractor[Audience] = ???
  val subject: KatCauseExtractor[Subject] = ???
  val location: KatCauseExtractor[ServerLocation] = ???
  val rotation: KatCauseExtractor[Vector3d] = ???
  val targetBlock: KatCauseExtractor[BlockSnapshot] = ???

  val player: KatCauseExtractor[ServerPlayer] = ???
  val entity: KatCauseExtractor[Entity] = ???
  val identity: KatCauseExtractor[Identity] = ???
  val nameable: KatCauseExtractor[Nameable] = ???
