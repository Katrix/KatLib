package net.katsstuff.katlib.commands

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import net.katsstuff.katlib.helpers._
import net.katsstuff.minejson.text.Text
import org.spongepowered.api.command.manager.CommandMapping
import org.spongepowered.api.command.parameter.managed.Flag
import org.spongepowered.api.command.parameter.{CommandContext, Parameter}
import org.spongepowered.api.command.{Command, CommandCause, CommandResult, CommandResults}
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent
import org.spongepowered.plugin.PluginContainer

case class KatCommandBuilder[R <: Tuple, F <: Tuple, P <: Tuple, CanSetParams <: Boolean, CanSetFlags <: Boolean] private[commands](
  parentObj: ParentParams[R],
  aliases: Seq[String],
  permissionObj: Option[String],
  executionRequirements: CommandCause => Boolean,
  extendedDescriptionFunc: CommandCause => Option[Text],
  shortDescriptionFunc: CommandCause => Option[Text],
  childrenSeq: Seq[KatCommand[Tuple.Concat[R, Tuple.Concat[F, P]], _, _]],
  flagsObj: KatFlags[F],
  parametersObj: KatParameters[P]
):

  def named(firstName: String, otherNames: String*): KatCommandBuilder[R, F, P, CanSetParams, CanSetFlags] =
    copy(aliases = firstName +: otherNames)

  def permission(permission: String): KatCommandBuilder[R, F, P, CanSetParams, CanSetFlags] =
    copy(permissionObj = Some(permission))

  def extendedDescription(f: CommandCause => Option[Text]): KatCommandBuilder[R, F, P, CanSetParams, CanSetFlags] =
    copy(extendedDescriptionFunc = f)
  def extendedDescription(text: Option[Text]): KatCommandBuilder[R, F, P, CanSetParams, CanSetFlags] =
    extendedDescription(_ => text)

  def shortDescription(f: CommandCause => Option[Text]): KatCommandBuilder[R, F, P, CanSetParams, CanSetFlags] =
    copy(shortDescriptionFunc = f)
  def shortDescription(text: Option[Text]): KatCommandBuilder[R, F, P, CanSetParams, CanSetFlags] =
    shortDescription(_ => text)

  def children(
    children: ParentParams[Tuple.Concat[R, Tuple.Concat[F, P]]] ?=> Seq[KatCommand[Tuple.Concat[R, Tuple.Concat[F, P]], _, _]]
  ): KatCommandBuilder[R, F, P, false, false] = copy(childrenSeq = children(using ParentParams.ParentParamsImpl(parentObj, flagsObj, parametersObj)))

  def flags[F2 <: Tuple](flags: KatFlags[F2])(using CanSetFlags =:= true): KatCommandBuilder[R, F2, P, CanSetParams, true] =
    copy(flagsObj = flags, childrenSeq = Nil)

  def parameters[P2 <: Tuple](parameters: KatParameters[P2])(using CanSetParams =:= true): KatCommandBuilder[R, F, P2, true, CanSetFlags] =
    copy(parametersObj = parameters, childrenSeq = Nil)
    
  def addParameter[P2 <: Tuple](extraParameters: ParentParams[Tuple.Concat[R, Tuple.Concat[F, P]]] ?=> KatParameters[P2])(using CanSetParams =:= true): KatCommandBuilder[R, F, Tuple.Concat[P, P2], true, false] =
    copy(
      parametersObj = parametersObj ~ extraParameters(using ParentParams.ParentParamsImpl(parentObj, flagsObj, parametersObj)), 
      childrenSeq = Nil
    )

  def handle(f: (CommandContext, Tuple.Concat[R, Tuple.Concat[F, P]]) => CommandResult): KatCommand[R, F, P] =
    KatCommand(
      parentObj,
      aliases,
      permissionObj,
      executionRequirements,
      extendedDescriptionFunc,
      shortDescriptionFunc,
      childrenSeq,
      flagsObj,
      parametersObj,
      f
    )
end KatCommandBuilder

case class KatCommand[R <: Tuple, F <: Tuple, P <: Tuple](
  parent: ParentParams[R],
  aliases: Seq[String],
  permission: Option[String],
  executionRequirements: CommandCause => Boolean,
  extendedDescription: CommandCause => Option[Text],
  shortDescription: CommandCause => Option[Text],
  children: Seq[KatCommand[Tuple.Concat[R, Tuple.Concat[F, P]], _, _]],
  flags: KatFlags[F],
  parameters: KatParameters[P],
  handler: (CommandContext, Tuple.Concat[R, Tuple.Concat[F, P]]) => CommandResult
) extends KatParameter[EmptyTuple]:
  
  def toSponge: Command.Parameterized = 
    val baseBuilder: Command.Builder = Command
      .builder()
      .setPermission(permission.orNull)
      .setExecutionRequirements(cause => executionRequirements(cause))
      .setExtendedDescription(cause => extendedDescription(cause).map(_.toSponge).toJava)
      .setShortDescription(cause => shortDescription(cause).map(_.toSponge).toJava)
      .children(children.map(c => c.aliases.asJava -> c.toSponge).toMap.asJava)
    
    for spongeFlag <- flags.toSpongeFlag do baseBuilder.flag(spongeFlag)
      
    baseBuilder.parameters(parameters.toSpongeParameter.asJava)
    baseBuilder.setExecutor { ctx => 
      val r = parent.getValuesFromContext(ctx)
      val f = flags.getValuesFromContext(ctx)
      val p = parameters.getValuesFromContext(ctx)
      
      handler(ctx, r ++ (f ++ p))
    }
    
    baseBuilder.build()
  end toSponge
  
  def register(event: RegisterCommandEvent[Command.Parameterized])(using container: PluginContainer): CommandMapping =
    event.register(container, toSponge, aliases.head, aliases.tail: _*)

  override def getValuesFromContext(context: CommandContext): EmptyTuple = EmptyTuple

  override def toSpongeParameter: Seq[Parameter] = Seq(
    Parameter.subcommand(toSponge, aliases.head, aliases.tail: _*)
  )
end KatCommand

case class StringFlagInfo(amountSpecified: Int)
case class ValueFlagInfo[A](values: Seq[A])

sealed trait KatFlags[F <: Tuple]:
  def ~[F2 <: Tuple](that: KatFlags[F2]): KatFlags[Tuple.Concat[F, F2]] =
    KatFlags.FlagConcat(this, that)
    
  def getValuesFromContext(context: CommandContext): F
    
  def toSpongeFlag: Seq[Flag]

object KatFlags:
  case object Empty extends KatFlags[EmptyTuple]:
    override def getValuesFromContext(context: CommandContext): EmptyTuple = EmptyTuple

    override def toSpongeFlag: Seq[Flag] = Nil
  
  case class FlagConcat[F1 <: Tuple, F2 <: Tuple](
    f1: KatFlags[F1], 
    f2: KatFlags[F2]
  ) extends KatFlags[Tuple.Concat[F1, F2]]:
    override def getValuesFromContext(context: CommandContext): Tuple.Concat[F1, F2] = 
      f1.getValuesFromContext(context) ++ f2.getValuesFromContext(context)

    override def toSpongeFlag: Seq[Flag] = f1.toSpongeFlag ++ f2.toSpongeFlag
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
  end StringFlag

  case class ParamFlag[A](
    aliases: Seq[String], 
    permission: Option[String], 
    parameter: KatParameter.ValueKatParameter[A, [Z] =>> Z]
  ) extends KatFlag[ValueFlagInfo[A]]:
    
    private val spongeFlag =
      Flag.builder().aliases(aliases.asJava).setPermission(permission.orNull).setParameter(parameter.valueParam).build()

    override def getValuesFromContext(context: CommandContext): Tuple1[ValueFlagInfo[A]] =
      Tuple1(ValueFlagInfo(context.getAll(parameter.valueParam.getKey).asScala.toSeq))

    override def toSpongeFlag: Seq[Flag] = Seq(spongeFlag)
  end ParamFlag
end KatFlag

sealed trait KatParameters[P <: Tuple]:
  def ~[P2 <: Tuple](that: KatParameters[P2]): KatParameters[Tuple.Concat[P, P2]] =
    KatParameters.ParameterConcat(this, that)
    
  def getValuesFromContext(context: CommandContext): P
  
  def toSpongeParameter: Seq[Parameter]

object KatParameters:
  case object Empty extends KatParameters[EmptyTuple]:
    override def getValuesFromContext(context: CommandContext): EmptyTuple = EmptyTuple

    override def toSpongeParameter: Seq[Parameter] = Nil
  
  case class ParameterConcat[P1 <: Tuple, P2 <: Tuple](
    p1: KatParameters[P1], 
    p2: KatParameters[P2]
  ) extends KatParameters[Tuple.Concat[P1, P2]]:
    override def getValuesFromContext(context: CommandContext): Tuple.Concat[P1, P2] =
      p1.getValuesFromContext(context) ++ p2.getValuesFromContext(context)

    override def toSpongeParameter: Seq[Parameter] = 
      p1.toSpongeParameter ++ p2.toSpongeParameter
end KatParameters

sealed trait KatParameter[A <: Tuple] extends KatParameters[A]
object KatParameter:
  type Id[A] = A
  
  sealed trait Amount[F[_]]:
    def getValuesFromContext[A](context: CommandContext, key: Parameter.Key[A]): F[A]
  
  object Amount:
    case object ZeroOrOne extends Amount[Option]:
      override def getValuesFromContext[A](context: CommandContext, key: Parameter.Key[A]): Option[A] =
        context.getOne(key).toScala
        
    case object ExactlyOne extends Amount[[A] =>> A]:
      override def getValuesFromContext[A](context: CommandContext, key: Parameter.Key[A]): A =
        context.requireOne(key)
    
    case object OneOrMore extends Amount[Seq]:
      override def getValuesFromContext[A](context: CommandContext, key: Parameter.Key[A]): Seq[A] =
        context.getAll(key).asScala.toSeq
  end Amount
  
  case class ValueKatParameter[A, F[_]](valueParam: Parameter.Value[A], amount: Amount[F]) extends KatParameter[Tuple1[F[A]]]:
    override def getValuesFromContext(context: CommandContext): Tuple1[F[A]] =
      Tuple1(amount.getValuesFromContext(context, valueParam.getKey))

    override def toSpongeParameter: Seq[Parameter] = Seq(valueParam)
  end ValueKatParameter
  
  val int: ValueKatParameter[Int, Id] = ???
  val string: ValueKatParameter[String, Id] = ???
  val player: ValueKatParameter[Player, Id] = ???
  val double: ValueKatParameter[Double, Id] = ???

sealed trait ParentParams[R <: Tuple]:
  def getValuesFromContext(ctx: CommandContext): R

object ParentParams: 
  private[commands] case class ParentParamsImpl[R <: Tuple, F <: Tuple, P <: Tuple](
    parent: ParentParams[R],
    flags: KatFlags[F],
    parameters: KatParameters[P]
  ) extends ParentParams[Tuple.Concat[R, Tuple.Concat[F, P]]]:
    override def getValuesFromContext(ctx: CommandContext): Tuple.Concat[R, Tuple.Concat[F, P]] =
      val r = parent.getValuesFromContext(ctx)
      val f = flags.getValuesFromContext(ctx)
      val p = parameters.getValuesFromContext(ctx)
      (r ++ (f ++ p))
  
  private[commands] case object NoParentParams extends ParentParams[EmptyTuple]:
    override def getValuesFromContext(ctx: CommandContext): EmptyTuple = EmptyTuple

object Commands:
  
  def topCommand(
    firstAlias: String, 
    aliases: String*
  ): KatCommandBuilder[EmptyTuple, EmptyTuple, EmptyTuple, true, true] =
    KatCommandBuilder(
      ParentParams.NoParentParams, 
      firstAlias +: aliases, 
      None, 
      _ => true,
      _ => None, 
      _ => None, 
      Nil, 
      KatFlags.Empty, 
      KatParameters.Empty
    )

  def childCommand[R <: Tuple](
    firstAlias: String, 
    aliases: String*
  )(using parent: ParentParams[R]): KatCommandBuilder[R, EmptyTuple, EmptyTuple, true, true] =
    KatCommandBuilder(
      parent,
      firstAlias +: aliases,
      None,
      _ => true,
      _ => None,
      _ => None,
      Nil,
      KatFlags.Empty,
      KatParameters.Empty
    )
end Commands

object Testing {

  Commands
    .topCommand("foo")
    .parameters(KatParameter.string ~ KatParameter.int)
    .children(
      Seq(
        Commands.childCommand("bar").handle { case (c, (str, int)) =>
          println(str.toLowerCase + int)
          CommandResults.SUCCESS
        },
        Commands.childCommand("baz").parameters(KatParameter.player).handle { case (c, (str, int, player)) =>
          println(str.toLowerCase + int)
          println(player.getName)
          CommandResults.SUCCESS
        }
      )
    )
  
  Commands
    .topCommand("foo")
    .parameters(KatParameter.string ~ KatParameter.int)
    .addParameter(Commands.childCommand("bar").handle { case (c, (str, int)) =>
      println(str.toLowerCase + int)
      CommandResults.SUCCESS
    })
    .handle { case (c, (str, int)) =>
      println(str.toLowerCase + int)
      CommandResults.SUCCESS
    }
}
