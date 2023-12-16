package net.katsstuff.katlib.spongecommands

import io.leangen.geantyref.TypeToken
import net.katsstuff.katlib.spongecommands.KatParameter.Amount
import net.katsstuff.minejson.text._
import net.katsstuff.katlib.helpers._
import org.checkerframework.checker.nullness.qual.{NonNull, Nullable}
import org.spongepowered.api.ResourceKey
import org.spongepowered.api.block.BlockState
import org.spongepowered.api.command.manager.CommandMapping
import org.spongepowered.api.command.parameter.managed.{ValueCompleter, ValueParser, ValueUsage}
import org.spongepowered.api.command.{Command, CommandCause, CommandResult}
import org.spongepowered.api.command.parameter.{CommandContext, Parameter}
import org.spongepowered.api.data.persistence.DataContainer
import org.spongepowered.api.entity.Entity
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.entity.living.player.server.ServerPlayer
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent
import org.spongepowered.api.item.inventory.ItemStackSnapshot
import org.spongepowered.api.registry.{DefaultedRegistryReference, RegistryHolder, RegistryType}
import org.spongepowered.api.util.Color
import org.spongepowered.math.vector.Vector3d
import org.spongepowered.plugin.PluginContainer
import cats.data.NonEmptyList
import net.katsstuff.katlib.spongecommands.KatParameters.MappedParameters
import perspective._
import perspective.derivation._

import java.lang.reflect.Method
import java.net.{InetAddress, URL}
import java.time.{Duration, LocalDateTime}
import java.util.UUID
import java.util.function.{Function, Predicate, Supplier}
import scala.annotation.nowarn
import scala.reflect.ClassTag
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

object opaques:
  opaque type ParameterValueBuilder[A] = AnyRef
  object ParameterValueBuilder:
    private val realClass = Class.forName("org.spongepowered.api.command.parameter.Parameter.Value.Builder")
    
    private val msetKeyStr = realClass.getMethod("setKey", classOf[String])
    private val msetKeykey = realClass.getMethod("setKey", classOf[Parameter.Key[_]])
    private val mparser = realClass.getMethod("parser", classOf[ValueParser[_]])
    private val msetSuggestions = realClass.getMethod("setSuggestions", classOf[ValueCompleter])
    private val msetUsage = realClass.getMethod("setUsage", classOf[ValueUsage])
    private val msetRequiredPermission = realClass.getMethod("setRequiredPermission", classOf[String])
    private val msetRequirements = realClass.getMethod("setRequirements", classOf[Predicate[_]])
    private val mconsumeAllRemaining = realClass.getMethod("consumeAllRemaining")
    private val moptional = realClass.getMethod("optional")
    private val morDefaultV = realClass.getMethod("orDefault", classOf[AnyRef])
    private val morDefaultS = realClass.getMethod("orDefault", classOf[Supplier[_]])
    private val morDefaultF = realClass.getMethod("orDefault", classOf[Function[_, _]])
    private val mterminal = realClass.getMethod("terminal")
    private val mbuild = realClass.getMethod("build")
    
    extension[T](self: ParameterValueBuilder[T])
      def setKey(key: String): ParameterValueBuilder[T] = msetKeyStr.invoke(self, key)
      def setKey(key: Parameter.Key[T]): ParameterValueBuilder[T] = msetKeykey.invoke(self, key)
      def parser(parser: ValueParser[_ <: T]): ParameterValueBuilder[T] = mparser.invoke(self, parser)
      //def parser[V <: ValueParser[_$1]](@NonNull parser: DefaultedRegistryReference[V]): Value.Builder[T] = this.parser(parser.get)
      def setSuggestions(@Nullable completer: ValueCompleter): ParameterValueBuilder[T] = msetSuggestions.invoke(self, completer)
      def setUsage(@Nullable usage: ValueUsage): ParameterValueBuilder[T] = msetUsage.invoke(self, usage)
      def setRequiredPermission(@Nullable permission: String): ParameterValueBuilder[T] = msetRequiredPermission.invoke(self, permission)
      def setRequirements(@Nullable executionRequirements: Predicate[CommandCause]): ParameterValueBuilder[T] = msetRequirements.invoke(self, executionRequirements)
      def consumeAllRemaining(): ParameterValueBuilder[T] = mconsumeAllRemaining.invoke(self)
      def optional(): ParameterValueBuilder[T] = moptional.invoke(self)
      def orDefault(@NonNull defaultValue: T): ParameterValueBuilder[T] = morDefaultV.invoke(self, defaultValue)
      def orDefault(defaultValueSupplier: Supplier[T]): ParameterValueBuilder[T] = morDefaultF.invoke(self, defaultValueSupplier)
      def orDefault(defaultValueFunction: Function[CommandCause, T]): ParameterValueBuilder[T] = morDefaultF.invoke(self, defaultValueFunction)
      def terminal(): ParameterValueBuilder[T] = mterminal.invoke(self)
      def build: Parameter.Value[T] = mbuild.invoke(self).asInstanceOf[Parameter.Value[T]]
export opaques._

sealed trait KatParameters[P <: Tuple]:
  def ~[P2 <: Tuple](that: KatParameters[P2]): KatParameters[Tuple.Concat[P, P2]] =
    KatParameters.ParameterConcat(this, that)

  def ~[P2](that: Parameter.Value[P2]): KatParameters[Tuple.Concat[P, Tuple1[P2]]] =
    KatParameters.ParameterConcat(this, that.toKat)

  def ~[P2](that: ParameterValueBuilder[P2]): KatParameters[Tuple.Concat[P, Tuple1[P2]]] =
    KatParameters.ParameterConcat(this, that.toKat)

  def getValuesFromContext(context: CommandContext): P

  def toSpongeParameter: Seq[Parameter]

  def simplify: KatParameters[P]

  def map[B](f: P => B): KatParameters[Tuple1[B]]

object KatParameters:
  private[spongecommands] case object Empty extends KatParameters[EmptyTuple]:
    override def getValuesFromContext(context: CommandContext): EmptyTuple = EmptyTuple

    override def toSpongeParameter: Seq[Parameter] = Nil

    override def simplify: KatParameters[EmptyTuple] = this

    override def map[B](f: EmptyTuple => B): KatParameters[Tuple1[B]] =
      MappedParameters(this, f)

  private[spongecommands] case class ParameterConcat[P1 <: Tuple, P2 <: Tuple](
    p1: KatParameters[P1],
    p2: KatParameters[P2]
  ) extends KatParameters[Tuple.Concat[P1, P2]]:
    override def getValuesFromContext(context: CommandContext): Tuple.Concat[P1, P2] =
      p1.getValuesFromContext(context) ++ p2.getValuesFromContext(context)

    override def toSpongeParameter: Seq[Parameter] =
      p1.toSpongeParameter ++ p2.toSpongeParameter

    override def simplify: KatParameters[Tuple.Concat[P1, P2]] =
      val simplifyResult = (p1.simplify, p2.simplify) match
        case (Empty, s2) => Left(s2.asInstanceOf[KatParameters[Tuple.Concat[P1, P2]]])
        case (s1, Empty) => Left(s1.asInstanceOf[KatParameters[Tuple.Concat[P1, P2]]])

        case (ParameterProductK(s1), ParameterProductK(s2)) =>
          Right((s1.tuple ++ s2.tuple))

        case (ParameterProductK(s1), s2: KatParameter[_]) =>
          Right((s1.tuple ++ Tuple1(s2)))

        case (s1: KatParameter[_], ParameterProductK(s2))  =>
          Right((s1 *: s2.tuple))

        case (s1: KatParameter[_], s2: KatParameter[_]) =>
          Right((s1, s2))

        case (s1, s2) => Left(ParameterConcat(s1, s2))

      simplifyResult match
        case Left(res)    => res
        case Right(value) =>
          val tuples = value.asInstanceOf[Tuple.Map[Tuple.Concat[P1, P2], KatParameter]]
          val size = tuples.size.asInstanceOf[Tuple.Size[Tuple.Concat[P1, P2]]]
          ParameterProductK(ProductK.of(tuples))(using ValueOf(size))

    override def map[B](f: Tuple.Concat[P1, P2] => B): KatParameters[Tuple1[B]] =
      MappedParameters(simplify, f)
  end ParameterConcat

  private[spongecommands] case class MappedParameters[A <: Tuple, B](orig: KatParameters[A], f: A => B) extends KatParameters[Tuple1[B]]:
    override def getValuesFromContext(context: CommandContext): Tuple1[B] = Tuple1(f(orig.getValuesFromContext(context)))

    override def toSpongeParameter: Seq[Parameter] = orig.toSpongeParameter

    override def simplify: KatParameters[Tuple1[B]] = copy(orig = orig.simplify)

    override def map[C](f: Tuple1[B] => C): KatParameters[Tuple1[C]] =
      copy(f = this.f.andThen(b => f(Tuple1(b))))

  private[spongecommands] case class ParameterProductK[P <: Tuple](
    parameters: ProductK[KatParameter, P]
  )(using ValueOf[Tuple.Size[P]]) extends KatParameters[P]:
    private inline def instance = ProductK.productKInstance[P]

    override def getValuesFromContext(context: CommandContext): P =
      instance
        .mapK[KatParameter, cats.Id, Nothing](parameters)(
          [Z] => (param: KatParameter[Z]) => param.getValuesFromContext(context)._1
        )
        .tuple
        .asInstanceOf[P]

    override def toSpongeParameter: Seq[Parameter] =
      instance.foldMapK[KatParameter, List[Parameter], Nothing](parameters)(
        [Z] => (param: KatParameter[Z]) => param.toSpongeParameter.toList
      )

    override def simplify: KatParameters[P] = this

    override def map[B](f: P => B): KatParameters[Tuple1[B]] =
      MappedParameters(this, f)
end KatParameters

trait KatParameter[A] extends KatParameters[Tuple1[A]]
object KatParameter extends KatParameterList:
  sealed trait Amount[F[_]]:
    def getValuesFromContext[A](context: CommandContext, key: Parameter.Key[A]): F[A]
    def lift[A, B](f: A => B): F[A] => F[B]

  object Amount:
    case object ZeroOrOne extends Amount[Option]:
      override def getValuesFromContext[A](context: CommandContext, key: Parameter.Key[A]): Option[A] =
        context.getOne(key).toScala

      override def lift[A, B](f: A => B): Option[A] => Option[B] =
        _.map(f)

    case object ExactlyOne extends Amount[[A] =>> A]:
      override def getValuesFromContext[A](context: CommandContext, key: Parameter.Key[A]): A =
        context.requireOne(key)

      override def lift[A, B](f: A => B): A => B = f

    case object OneOrMore extends Amount[NonEmptyList]:
      override def getValuesFromContext[A](context: CommandContext, key: Parameter.Key[A]): NonEmptyList[A] =
        NonEmptyList.fromListUnsafe(context.getAll(key).asScala.toList)

      override def lift[A, B](f: A => B): NonEmptyList[A] => NonEmptyList[B] =
        _.map(f)

    case object ZeroOrMore extends Amount[Seq]:
      override def getValuesFromContext[A](context: CommandContext, key: Parameter.Key[A]): Seq[A] =
        context.getAll(key).asScala.toSeq

      override def lift[A, B](f: A => B): Seq[A] => Seq[B] = _.map(f)
  end Amount
  export Amount._

  case class KatValueParameter[A, F[_]](
    valueParam: Parameter.Value[A],
    amount: Amount[F] = Amount.ExactlyOne
  ) extends KatParameter[F[A]]:
    override def getValuesFromContext(context: CommandContext): Tuple1[F[A]] =
      Tuple1(amount.getValuesFromContext(context, valueParam.getKey))
  
    override def toSpongeParameter: Seq[Parameter] = Seq(valueParam)
  
    override def simplify: KatParameters[Tuple1[F[A]]] = this
  
    override def map[B](f: (Tuple1[F[A]]) => B): KatParameters[Tuple1[B]] =
      KatParameters.MappedParameters(this, f)
  end KatValueParameter

  type ChangableKatValueParameter[A, F[_]] = ComplexChangableKatValueParameter[A, A, F]
  case class ComplexChangableKatValueParameter[A, B, F[_]](
      valueParamBuilder: () => ParameterValueBuilder[A],
      amount: Amount[F] = Amount.ExactlyOne,
      f: A => B
  ) extends KatParameter[F[B]]:
    import reflect.Selectable.reflectiveSelectable
    
    lazy val valueParam: Parameter.Value[A] = valueParamBuilder().build
    
    override def getValuesFromContext(context: CommandContext): Tuple1[F[B]] =
      Tuple1((amount.lift(f))(amount.getValuesFromContext(context, valueParam.getKey)))
  
    override def toSpongeParameter: Seq[Parameter] = Seq(valueParam)
  
    override def simplify: KatParameters[Tuple1[F[B]]] = this
  
    override def map[C](f: (Tuple1[F[B]]) => C): KatParameters[Tuple1[C]] =
      MappedParameters(this, f)
      
    def vmap[C](f: B => C): ComplexChangableKatValueParameter[A, C, F] =
      copy(f = this.f.andThen(f))
      
    def editParam(f: ParameterValueBuilder[A] => ParameterValueBuilder[A]): ComplexChangableKatValueParameter[A, B, F] =
      copy(valueParamBuilder = () => f(valueParamBuilder()))
      
    def named(name: String): ComplexChangableKatValueParameter[A, B, F] =
      editParam(_.setKey(name))
      
    def optional(using F[Unit] =:= Unit): ComplexChangableKatValueParameter[A, B, Option] =
      copy(valueParamBuilder = () => valueParamBuilder().optional(), amount = ZeroOrOne)

    def consumeAllRemaining(using F[Unit] =:= Unit): ComplexChangableKatValueParameter[A, B, NonEmptyList] =
      copy(valueParamBuilder = () => valueParamBuilder().consumeAllRemaining(), amount = OneOrMore)

    def optionalConsumeAllRemaining(using F[Unit] =:= Unit): ComplexChangableKatValueParameter[A, B, Seq] =
      copy(valueParamBuilder = () => valueParamBuilder().optional().consumeAllRemaining(), amount = ZeroOrMore)
  end ComplexChangableKatValueParameter
end KatParameter

case class KatCommand[R <: Tuple, C <: Tuple, F <: Tuple, P <: Tuple](
  parent: ParentParams[R],
  aliases: Seq[String],
  permissionObj: Option[String],
  executionRequirementsFunc: CommandCause => Boolean,
  extendedDescriptionFunc: CommandCause => Option[Text],
  shortDescriptionFunc: CommandCause => Option[Text],
  children: Seq[KatCommand[R, _, _, _]],
  causeExtractor: KatCauseExtractors[C],
  flags: KatFlags[F],
  parameters: KatParameters[P],
  handler: (CommandContext, CommandTuple[R, C, F, P]) => Either[String, CommandResult]
) extends KatParameters[EmptyTuple]:

  def named(firstName: String, otherNames: String*): KatCommand[R, C, F, P] =
    copy(aliases = firstName +: otherNames)

  def permission(permission: String): KatCommand[R, C, F, P] =
    copy(permissionObj = Some(permission))

  def extendedDescription(f: CommandCause => Option[Text]): KatCommand[R, C, F, P] =
    copy(extendedDescriptionFunc = f)
  def extendedDescription(text: Option[Text]): KatCommand[R, C, F, P] =
    extendedDescription(_ => text)

  def shortDescription(f: CommandCause => Option[Text]): KatCommand[R, C, F, P] =
    copy(shortDescriptionFunc = f)
  def shortDescription(text: Option[Text]): KatCommand[R, C, F, P] =
    shortDescription(_ => text)

  def toSponge: Command.Parameterized =
    val baseBuilder: Command.Builder = Command
      .builder()
      .setPermission(permissionObj.orNull)
      .setExecutionRequirements(cause => executionRequirementsFunc(cause))
      .setExtendedDescription(cause => extendedDescriptionFunc(cause).map(_.toSponge).toJava)
      .setShortDescription(cause => shortDescriptionFunc(cause).map(_.toSponge).toJava)
      .children(children.map(c => c.aliases.asJava -> c.toSponge).toMap.asJava)

    for spongeFlag <- flags.toSpongeFlag do baseBuilder.flag(spongeFlag)

    baseBuilder.parameters(parameters.toSpongeParameter.asJava)
    baseBuilder.setExecutor { ctx =>
      val r = parent.getValuesFromContext(ctx)
      val c = causeExtractor.getValuesFromContext(ctx)
      val f = flags.getValuesFromContext(ctx)
      val p = parameters.getValuesFromContext(ctx)

      handler(ctx, r ++ (c ++ (f ++ p))) match
        case Right(success) => success
        case Left(error) => CommandResult.error(t"${Red}${error}".toSponge)
    }

    baseBuilder.build()
  end toSponge

  def register(event: RegisterCommandEvent[Command.Parameterized])(using container: PluginContainer): CommandMapping =
    event.register(container, toSponge, aliases.head, aliases.tail: _*).mapping()

  override def getValuesFromContext(context: CommandContext): EmptyTuple = EmptyTuple

  override def toSpongeParameter: Seq[Parameter] = Seq(
    Parameter.subcommand(toSponge, aliases.head, aliases.tail: _*)
  )

  override def simplify: KatCommand[R, C, F, P] =
    copy(
      parent = parent.simplify,
      children = children.map(_.simplify),
      flags = flags.simplify,
      parameters = parameters.simplify
    )

  override def map[B](f: EmptyTuple => B): KatParameters[Tuple1[B]] =
    KatParameters.MappedParameters(this, f)
end KatCommand
