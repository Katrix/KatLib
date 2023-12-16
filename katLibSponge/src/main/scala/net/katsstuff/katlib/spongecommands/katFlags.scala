package net.katsstuff.katlib.spongecommands

import scala.jdk.CollectionConverters._

import org.spongepowered.api.command.CommandCause
import org.spongepowered.api.command.parameter.CommandContext
import org.spongepowered.api.command.parameter.managed.Flag
import perspective._
import perspective.derivation._

case class StringFlagInfo(amountSpecified: Int)
case class ValueFlagInfo[A](values: Seq[A])

sealed trait KatFlags[F <: Tuple]:
  def ~[F2 <: Tuple](that: KatFlags[F2]): KatFlags[Tuple.Concat[F, F2]] =
    KatFlags.FlagConcat(this, that)

  def getValuesFromContext(context: CommandContext): F

  def toSpongeFlag: Seq[Flag]

  def simplify: KatFlags[F]

  def map[B](f: F => B): KatFlags[Tuple1[B]]

object KatFlags:
  private[spongecommands] case object Empty extends KatFlags[EmptyTuple]:
    override def getValuesFromContext(context: CommandContext): EmptyTuple = EmptyTuple

    override def toSpongeFlag: Seq[Flag] = Nil

    override def simplify: KatFlags[EmptyTuple] = this

    override def map[B](f: EmptyTuple => B): KatFlags[Tuple1[B]] = MappedFlags(this, f)

  private[spongecommands] case class FlagConcat[F1 <: Tuple, F2 <: Tuple](
    f1: KatFlags[F1],
    f2: KatFlags[F2]
  ) extends KatFlags[Tuple.Concat[F1, F2]]:
    override def getValuesFromContext(context: CommandContext): Tuple.Concat[F1, F2] =
      f1.getValuesFromContext(context) ++ f2.getValuesFromContext(context)

    override def toSpongeFlag: Seq[Flag] = f1.toSpongeFlag ++ f2.toSpongeFlag

    override def simplify: KatFlags[Tuple.Concat[F1, F2]] =
      val simplifyResult = (f1.simplify, f2.simplify) match
        case (Empty, s2) => Left(s2.asInstanceOf[KatFlags[Tuple.Concat[F1, F2]]])
        case (s1, Empty) => Left(s1.asInstanceOf[KatFlags[Tuple.Concat[F1, F2]]])

        case (FlagProductK(s1), FlagProductK(s2)) =>
          Right((s1.tuple ++ s2.tuple))

        case (FlagProductK(s1), s2: KatFlag[_]) =>
          Right((s1.tuple ++ Tuple1(s2)))

        case (s1: KatFlag[_], FlagProductK(s2))  =>
          Right((s1 *: s2.tuple))

        case (s1: KatFlag[_], s2: KatFlag[_]) =>
          Right((s1, s2))

        case (s1, s2) => Left(FlagConcat(s1, s2))

      simplifyResult match
        case Left(res)    => res
        case Right(value) =>
          val tuples = value.asInstanceOf[Tuple.Map[Tuple.Concat[F1, F2], KatFlag]]
          val size = tuples.size.asInstanceOf[Tuple.Size[Tuple.Concat[F1, F2]]]
          FlagProductK(ProductK.of(tuples))(using ValueOf(size))

    override def map[B](f: Tuple.Concat[F1, F2] => B): KatFlags[Tuple1[B]] = MappedFlags(simplify, f)

  private[spongecommands] case class MappedFlags[A <: Tuple, B](orig: KatFlags[A], f: A => B) extends KatFlags[Tuple1[B]]:
    override def getValuesFromContext(context: CommandContext): Tuple1[B] =
      Tuple1(f(orig.getValuesFromContext(context)))

    override def toSpongeFlag: Seq[Flag] = orig.toSpongeFlag

    override def simplify: KatFlags[Tuple1[B]] = copy(orig = orig.simplify)

    override def map[C](f: (Tuple1[B]) => C): KatFlags[Tuple1[C]] =
      copy(f = this.f.andThen(b => f(Tuple1(b))))

  private[spongecommands] case class FlagProductK[F <: Tuple](flags: ProductK[KatFlag, F])(using ValueOf[Tuple.Size[F]]) extends KatFlags[F]:
    private inline def instance = ProductK.productKInstance[F]

    override def getValuesFromContext(context: CommandContext): F =
      instance
        .mapK[KatFlag, cats.Id, Nothing](flags)([Z] => (flag: KatFlag[Z]) => flag.getValuesFromContext(context)._1)
    .tuple
      .asInstanceOf[F]

    override def toSpongeFlag: Seq[Flag] =
      instance.foldMapK[KatFlag, List[Flag], Nothing](flags)([Z] => (flag: KatFlag[Z]) => flag.toSpongeFlag.toList)

    override def simplify: KatFlags[F] = this

    override def map[B](f: F => B): KatFlags[Tuple1[B]] = MappedFlags(this, f)
end KatFlags

sealed trait KatFlag[F] extends KatFlags[Tuple1[F]]
object KatFlag:
  case class StringFlag(
    aliases: Seq[String],
    permission: Option[String],
    requirements: CommandCause => Boolean
  ) extends KatFlag[StringFlagInfo]:
    private val spongeFlag =
      Flag
        .builder()
        .aliases(aliases.asJava)
        .setPermission(permission.orNull)
        .setRequirement(cause => requirements(cause))
        .build()

    override def getValuesFromContext(context: CommandContext): Tuple1[StringFlagInfo] =
      Tuple1(StringFlagInfo(context.getFlagInvocationCount(spongeFlag)))

    override def toSpongeFlag: Seq[Flag] = Seq(spongeFlag)

    override def simplify: KatFlags[Tuple1[StringFlagInfo]] = this

    override def map[B](f: (Tuple1[StringFlagInfo]) => B): KatFlags[Tuple1[B]] =
      KatFlags.MappedFlags(this, f)
  end StringFlag

  case class ParamFlag[A](
    aliases: Seq[String],
    permission: Option[String],
    parameter: KatParameter.KatValueParameter[A, [Z] =>> Z]
  ) extends KatFlag[ValueFlagInfo[A]]:

    private val spongeFlag =
      Flag
        .builder()
        .aliases(aliases.asJava)
        .setPermission(permission.orNull)
        .setParameter(parameter.valueParam)
        .build()

    override def getValuesFromContext(context: CommandContext): Tuple1[ValueFlagInfo[A]] =
      Tuple1(ValueFlagInfo(context.getAll(parameter.valueParam.getKey).asScala.toSeq))

    override def toSpongeFlag: Seq[Flag] = Seq(spongeFlag)

    override def simplify: KatFlags[Tuple1[ValueFlagInfo[A]]] = this

    override def map[B](f: (Tuple1[ValueFlagInfo[A]]) => B): KatFlags[Tuple1[B]] =
      KatFlags.MappedFlags(this, f)
  end ParamFlag
end KatFlag
