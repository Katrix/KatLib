package net.katsstuff.katlib.spongecommands

import io.leangen.geantyref.TypeToken
import net.katsstuff.katlib.KatLib

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import scala.annotation.targetName
import perspective._
import perspective.derivation._
import net.katsstuff.katlib.helpers._
import net.katsstuff.minejson.text._
import net.kyori.adventure.audience.Audience
import org.spongepowered.api.ResourceKey
import org.spongepowered.api.block.{BlockSnapshot, BlockState}
import org.spongepowered.api.command.exception.CommandException
import org.spongepowered.api.command.manager.CommandMapping
import org.spongepowered.api.command.parameter.managed.Flag
import org.spongepowered.api.command.parameter.{CommandContext, Parameter}
import org.spongepowered.api.command.{Command, CommandCause, CommandResult, CommandResults}
import org.spongepowered.api.data.persistence.DataContainer
import org.spongepowered.api.entity.Entity
import org.spongepowered.api.entity.living.player.{Player, User}
import org.spongepowered.api.entity.living.player.server.ServerPlayer
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent
import org.spongepowered.api.item.inventory.ItemStackSnapshot
import org.spongepowered.api.registry.{RegistryHolder, RegistryType}
import org.spongepowered.api.service.permission.Subject
import org.spongepowered.api.util.{Color, Nameable}
import org.spongepowered.math.vector.Vector3d
import org.spongepowered.plugin.PluginContainer

import java.net.{InetAddress, URL}
import java.time.{Duration, LocalDateTime}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}
import cats.data.NonEmptyList
import cats.effect.IO
import net.kyori.adventure.identity.Identity

type CommandTuple[R <: Tuple, C <: Tuple, F <: Tuple, P <: Tuple] = Tuple.Concat[R, Tuple.Concat[C, Tuple.Concat[F, P]]]

case class KatCommandBuilder[
  R <: Tuple, 
  C <: Tuple, 
  F <: Tuple, 
  P <: Tuple, 
  CanSetAllParams <: Boolean,
  CanSetFlags <: Boolean
] private[spongecommands](
  parentObj: ParentParams[R],
  aliases: Seq[String],
  permissionObj: Option[String],
  executionRequirements: CommandCause => Boolean,
  extendedDescriptionFunc: CommandCause => Option[Text],
  shortDescriptionFunc: CommandCause => Option[Text],
  childrenSeq: Seq[KatCommand[R, _, _, _]],
  causeExtractorObj: KatCauseExtractors[C],
  flagsObj: KatFlags[F],
  parametersObj: KatParameters[P]
):

  def named(firstName: String, otherNames: String*): KatCommandBuilder[R, C, F, P, CanSetAllParams, CanSetFlags] =
    copy(aliases = firstName +: otherNames)

  def permission(permission: String): KatCommandBuilder[R, C, F, P, CanSetAllParams, CanSetFlags] =
    copy(permissionObj = Some(permission))

  def extendedDescription(f: CommandCause => Option[Text]): KatCommandBuilder[R, C, F, P, CanSetAllParams, CanSetFlags] =
    copy(extendedDescriptionFunc = f)
  def extendedDescription(text: Option[Text]): KatCommandBuilder[R, C, F, P, CanSetAllParams, CanSetFlags] =
    extendedDescription(_ => text)

  def shortDescription(f: CommandCause => Option[Text]): KatCommandBuilder[R, C, F, P, CanSetAllParams, CanSetFlags] =
    copy(shortDescriptionFunc = f)
  def shortDescription(text: Option[Text]): KatCommandBuilder[R, C, F, P, CanSetAllParams, CanSetFlags] =
    shortDescription(_ => text)
    
  def children(
    children: ParentParams[R] ?=> Seq[KatCommand[R, _, _, _]]
  ): KatCommandBuilder[R, C, F, P, CanSetAllParams, CanSetFlags] = 
    copy(childrenSeq = children(using parentObj))
    
  def causeExtractors[C2 <: Tuple](extractors: KatCauseExtractors[C2]): KatCommandBuilder[R, C2, F, P, CanSetAllParams, CanSetFlags] =
    copy(causeExtractorObj = extractors)

  def flags[F2 <: Tuple](flags: KatFlags[F2])(using CanSetFlags =:= true): KatCommandBuilder[R, C, F2, P, CanSetAllParams, true] =
    copy(flagsObj = flags, childrenSeq = Nil)

  def parameters[P2 <: Tuple](parameters: KatParameters[P2])(using CanSetAllParams =:= true): KatCommandBuilder[R, C, F, P2, true, CanSetFlags] =
    copy(parametersObj = parameters, childrenSeq = Nil)
    
  def addParameter[P2 <: Tuple](
    extraParameters: ParentParams[CommandTuple[R, C, F, P]] ?=> KatParameters[P2]
  ): KatCommandBuilder[R, C, F, Tuple.Concat[P, P2], false, false] = 
    val thisAsParent = ParentParams.ParentParamsImpl(parentObj, causeExtractorObj, flagsObj, parametersObj).simplify
    copy(
      parametersObj = parametersObj ~ extraParameters(using thisAsParent), 
      childrenSeq = Nil
    )

  def handle(f: (CommandContext, CommandTuple[R, C, F, P]) => CommandResult): KatCommand[R, C, F, P] =
    handleEither((ctx, tuple) => Right(f(ctx, tuple)))

  def handleAsync(f: (CommandContext, CommandTuple[R, C, F, P]) => Future[Unit])(using ExecutionContext): KatCommand[R, C, F, P] =
    handleEither { (ctx, tuple) =>
      f(ctx, tuple).onComplete {
        case Success(_) =>
        case Failure(e) => 
          ctx.sendMessageCtx(t"${Red}Failed to run command. ${e.getMessage}")
          KatLib.logger.error("Error when executing async command", e)
      }
      Right(CommandResults.EMPTY)
    }

  def handleAsyncIO(f: (CommandContext, CommandTuple[R, C, F, P]) => IO[CommandResult])(using ExecutionContext): KatCommand[R, C, F, P] =
    handleEither { (ctx, tuple) =>
      val res = f(ctx, tuple).unsafeToFuture()
      
      res.value match
        case Some(Success(res)) => Right(res)
        case Some(Failure(e))   => throw e
        case None               => 
          res.onComplete {
            case Success(_) =>
            case Failure(e) =>
              ctx.sendMessageCtx(t"${Red}Failed to run command. ${e.getMessage}")
              KatLib.logger.error("Error when executing async command", e)
          }
          Right(CommandResults.EMPTY)
    }

  def handleEitherAsyncIO(f: (CommandContext, CommandTuple[R, C, F, P]) => IO[Either[String, CommandResult]])(using ExecutionContext): KatCommand[R, C, F, P] =
    handleEither { (ctx, tuple) =>
      val res = f(ctx, tuple).unsafeToFuture()

      res.value match
        case Some(Success(res)) => res
        case Some(Failure(e))   => throw e
        case None               =>
          res.onComplete {
            case Success(_) =>
            case Failure(e) =>
              ctx.sendMessageCtx(t"${Red}Failed to run command. ${e.getMessage}")
              KatLib.logger.error("Error when executing async command", e)
          }
          Right(CommandResults.EMPTY)
    }

  def handleEither(f: (CommandContext, CommandTuple[R, C, F, P]) => Either[String, CommandResult]): KatCommand[R, C, F, P] =
    KatCommand(
      parentObj,
      aliases,
      permissionObj,
      executionRequirements,
      extendedDescriptionFunc,
      shortDescriptionFunc,
      childrenSeq,
      causeExtractorObj,
      flagsObj,
      parametersObj,
      f
    )
end KatCommandBuilder

object Commands:
  
  def topCommand(
    firstAlias: String, 
    aliases: String*
  ): KatCommandBuilder[EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple, true, true] =
    KatCommandBuilder(
      ParentParams.NoParentParams, 
      firstAlias +: aliases, 
      None, 
      _ => true,
      _ => None, 
      _ => None, 
      Nil,
      KatCauseExtractors.Empty,
      KatFlags.Empty, 
      KatParameters.Empty
    )

  def childCommand[R <: Tuple](
    firstAlias: String, 
    aliases: String*
  )(using parent: ParentParams[R]): KatCommandBuilder[R, EmptyTuple, EmptyTuple, EmptyTuple, true, true] =
    KatCommandBuilder(
      parent,
      firstAlias +: aliases,
      None,
      _ => true,
      _ => None,
      _ => None,
      Nil,
      KatCauseExtractors.Empty,
      KatFlags.Empty,
      KatParameters.Empty
    )
end Commands

extension [A, B <: Tuple](par: => ParameterValueBuilder[A])
  inline def ~(other: KatParameters[B]): KatParameters[Tuple.Concat[Tuple1[A], B]] =
    par.toKat ~ other

extension [A, B](par: => ParameterValueBuilder[A])
  inline def ~(other: ParameterValueBuilder[B]): KatParameters[(A, B)] =
    par.toKat ~ other.toKat

extension [A, B](par: => ParameterValueBuilder[A])
  inline def ~(other: Parameter.Value[B]): KatParameters[(A, B)] =
    par.toKat ~ other.toKat

extension [A](par: => ParameterValueBuilder[A])
  inline def toKat: KatParameters[Tuple1[A]] =
    KatParameter(par)
    
  inline def toKatOptional: KatParameters[Tuple1[Option[A]]] =
    KatParameter(par, KatParameter.ZeroOrOne)

  inline def toKatMany: KatParameters[Tuple1[NonEmptyList[A]]] =
    KatParameter(par, KatParameter.OneOrMore)
    
  inline def toKatSome: KatParameters[Tuple1[Seq[A]]] =
    KatParameter(par, KatParameter.ZeroOrMore)

extension [A, B <: Tuple](par: Parameter.Value[A])
  inline def ~(other: KatParameters[B]): KatParameters[Tuple.Concat[Tuple1[A], B]] =
    par.toKat ~ other

extension [A, B](par: Parameter.Value[A])
  inline def ~(other: Parameter.Value[B]): KatParameters[(A, B)] =
    par.toKat ~ other.toKat

extension [A, B](par: Parameter.Value[A])
  inline def ~(other: ParameterValueBuilder[B]): KatParameters[(A, B)] =
    par.toKat ~ other.toKat

extension [A](par: Parameter.Value[A])
  inline def toKat: KatParameters[Tuple1[A]] =
    KatParameter.KatValueParameter(par)

  inline def toKatOptional: KatParameters[Tuple1[Option[A]]] =
    KatParameter.KatValueParameter(par, KatParameter.ZeroOrOne)

  inline def toKatMany: KatParameters[Tuple1[NonEmptyList[A]]] =
    KatParameter.KatValueParameter(par, KatParameter.OneOrMore)

  inline def toKatSome: KatParameters[Tuple1[Seq[A]]] =
    KatParameter.KatValueParameter(par, KatParameter.ZeroOrMore)

object Testing {

  Commands
    .topCommand("foo")
    .parameters(KatParameter.string ~ KatParameter.int)
    .addParameter(
      Commands.childCommand("bar").handle { case (c, (str, int)) =>
        println(str.toLowerCase + int)
        CommandResults.SUCCESS
      }
    )
    .addParameter(
      Commands.childCommand("baz").parameters(KatParameter.player).handle { case (c, (str, int, player)) =>
        println(str.toLowerCase + int)
        println(player.getName)
        CommandResults.SUCCESS
      }
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
