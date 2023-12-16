package net.katsstuff.homesweethome.cmd

import net.katsstuff.homesweethome.HomeHandler
import net.katsstuff.homesweethome.lib.LibPerm
import net.katsstuff.katlib.spongecommands.{Commands, KatCauseExtractor, KatParameter}
import net.katsstuff.katlib.helpers._
import net.katsstuff.minejson.text._
import net.kyori.adventure.identity.Identified
import org.spongepowered.api.ResourceKey
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.parameter.Parameter
import org.spongepowered.api.entity.Entity
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.entity.living.player.server.ServerPlayer
import org.spongepowered.api.service.permission.Subject
import org.spongepowered.api.world.server.ServerLocation
import org.spongepowered.math.vector.Vector3d

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

type SpongeHomeCmdImpl = HomeCmdsImpl[CommandResult, Subject, ServerLocation, Vector3d, Entity, ResourceKey]

def home(using homeHandler: HomeHandler[Subject, ResourceKey]): KatParameter[GenHomeWithName[ResourceKey]] =
  ???
  /*
  Parameter
    .builder(classOf[HomeWithName])
    .setKey("home")
    .parser { case (key, reader, ctx) =>
      val ret = ctx.getCause.first(classOf[ServerPlayer]).toScala.toRight("Need to be a player to execute this command").flatMap { player =>
        val pos      = reader.getCursor
        val homeName = reader.parseString()
      
        val ret = homeHandler.getHomeInCache(player.identity, reader).toRight("").orElse {
          val partialMatches = 
            homeHandler
              .allHomesForIdentInCache(player.identity)
              .filter(_._1.regionMatches(true, 0, currentInput, 0, currentInput.length))
              .map(_._2)
              .toSeq
          
          if partialMatches.length > 1 then 
            Left(s"More than one home matches $homeName") 
          else 
            partialMatches.headOption.toRight(s"No homes named $homeName found")
        }
      }.fold(e => throw ArgumentParseException(t"${Red}$e", reader.getInput, pos), identity)
      
      Some(ret).asJava
    }
    .setSuggestions { case (ctx, currentInput) => 
      ctx.getCause.first(classOf[ServerPlayer]).toScala.fold(Nil) { player =>
        homeHandler.allHomesForIdentInCache(player.identity).keys.filter(_.regionMatches(true, 0, currentInput, 0, currentInput.length))
      }.asJava
    }
    .build()
  */

def otherHome(userkey: Parameter.Key[User])(using homeHandler: HomeHandler[Subject, ResourceKey]): KatParameter[GenOtherHome[ResourceKey]] = 
  ???
  /*
  Parameter
    .builder(classOf[OtherHome])
    .setKey("other-home")
    .parser { case (key, reader, ctx) =>
      val user = ctx.requireOne(userkey)

      val pos      = reader.getCursor
      val homeName = reader.parseString()

      val ret = homeHandler.getHomeInCache(user.getProfile, reader).toRight("").orElse {
        val partialMatches =
          homeHandler
            .allHomesForIdentInCache(user.getProfile)
            .filter(_._1.regionMatches(true, 0, currentInput, 0, currentInput.length))
            .map(_._2)
            .toSeq

        if partialMatches.length > 1 then
          Left(s"More than one home matches $homeName")
        else
          partialMatches.headOption.toRight(s"No homes named $homeName found")
      }.fold(e => throw ArgumentParseException(t"${Red}$e", reader.getInput, pos), identity)

      Some(ret).asJava
    }
    .setSuggestions { case (ctx, currentInput) =>
      if ctx.hasPermission(LibPerm.HomeOtherList) then
        ctx.getOne(userkey).toScala.fold(Nil.asJava) { user =>
          homeHandler
            .allHomesForIdentInCache(user.getProfile)
            .keys
            .filter(_.regionMatches(true, 0, currentInput, 0, currentInput.length))
        }
      else
        Nil.asJava
    }
    .build()
  */

def homesCommand(using ExecutionContext)(using impl: SpongeHomeCmdImpl) =
  Commands
    .topCommand("homes")
    .permission(LibPerm.HomeList)
    .shortDescription(Some(t"Lists all of your current homes"))
    .causeExtractors(KatCauseExtractor.audience ~ KatCauseExtractor.identity)
    .handleAsyncIO { case (ctx, (audience, identity)) => impl.listHomes(audience, identity, isOther = false) }

def homeCommand(using ExecutionContext, HomeHandler[Subject, ResourceKey])(using impl: SpongeHomeCmdImpl) =
  Commands
    .topCommand("home")
    .parameters(home)
    .addParameter(
      Commands
        .childCommand("delete")
        .causeExtractors(KatCauseExtractor.audience ~ KatCauseExtractor.identity)
        .permission(LibPerm.HomeDelete)
        .shortDescription(Some(t"Deletes a home"))
        .handle { case (ctx, (home, audience, homeOwner)) => impl.deleteHome(audience, GenOtherHome.same(home, homeOwner)) }
    )
    .addParameter(
      Commands
        .childCommand("invite")
        .causeExtractors(KatCauseExtractor.audience ~ KatCauseExtractor.identity ~ KatCauseExtractor.nameable)
        .parameters(KatParameter.player)
        .permission(LibPerm.HomeInvite)
        .shortDescription(Some(t"Invite someone to your home"))
        .handle { case (ctx, (home, audience, homeOwner, inviterNamed, player)) => 
          impl.inviteToHome(audience, inviterNamed.getName, GenOtherHome.same(home, homeOwner), player.identity) 
        }
    )
    .addParameter(
      Commands
        .childCommand("residents")
        .children(
          Seq(
            Commands
              .childCommand("add")
              .causeExtractors(KatCauseExtractor.audience ~ KatCauseExtractor.identity)
              .parameters(KatParameter.user)
              .permission(LibPerm.HomeResidentAdd)
              .shortDescription(Some(t"Add a user as a resident to a home"))
              .handleEither { case (ctx, (home, audience, homeOwner, user)) => 
                impl.addResident(audience, GenOtherHome.same(home, homeOwner), user.getProfile) 
              },
            Commands
              .childCommand("remove")
              .causeExtractors(KatCauseExtractor.audience ~ KatCauseExtractor.identity)
              .parameters(KatParameter.user)
              .permission(LibPerm.HomeResidentRemove)
              .shortDescription(Some(t"Remove a user as a resident from a home"))
              .handleEither { case (ctx, (home, audience, homeOwner, user)) => 
                impl.removeResident(audience, GenOtherHome.same(home, homeOwner), user.getProfile) 
              }
          )
        )
        .causeExtractors(KatCauseExtractor.audience ~ KatCauseExtractor.identity)
        .permission(LibPerm.HomeResidentList)
        .shortDescription(Some(t"List the residents of a home"))
        .handle { case (ctx, (home, audience, homeOwner)) => 
          impl.listHomeResidents(audience, GenOtherHome.same(home, homeOwner)) 
        }
    )
    .children(
      Seq(
        homesCommand.named("list"),
        Commands
          .topCommand("set")
          .permission(LibPerm.HomeSet)
          .shortDescription(Some(t"Set a new home where you are standing"))
          .causeExtractors(
            KatCauseExtractor.audience ~
              KatCauseExtractor.location ~
              KatCauseExtractor.rotation ~
              KatCauseExtractor.identity
          )
          .parameters(KatParameter.string/*.named("home")*/)
          .handleEitherAsyncIO { case (ctx, (audience, location, rotation, homeOwner, homeName)) =>
            impl.setHome(audience, location, rotation, homeOwner, homeName, isOther = false)
          },
        Commands
          .topCommand("other")
          .parameters(KatParameter.user)
          .addParameter(
            Commands
              .childCommand("set")
              .causeExtractors(
                KatCauseExtractor.audience ~
                  KatCauseExtractor.location ~
                  KatCauseExtractor.rotation
              )
              .parameters(KatParameter.string)
              .permission(LibPerm.HomeOtherSet)
              .shortDescription(Some(t"Set a new home for someone else where you are standing"))
              .handleEitherAsyncIO { case (ctx, (homeOwner, audience, location, rotation, homeName)) => 
                impl.setHome(audience, location, rotation, homeOwner.getProfile, homeName, isOther = true) 
              }
          )
          .addParameter(
            Commands
              .childCommand("list")
              .causeExtractors(KatCauseExtractor.audience)
              .permission(LibPerm.HomeOtherList)
              .shortDescription(Some(t"Lists all of someone's current homes"))
              .handleAsyncIO { case (ctx, (homeOwner, audience)) => impl.listHomes(audience, homeOwner.getProfile, isOther = true) }
          )
          .addParameter(
            Commands
              .childCommand("residents")
              .causeExtractors(KatCauseExtractor.audience)
              .permission(LibPerm.HomeOtherResidentList)
              .shortDescription(Some(t"List all residents for someone"))
              .handleAsyncIO { case (ctx, (homeOwner, audience)) => impl.listResidents(audience, homeOwner.getProfile, isOther = true) }
          )
          .addParameter(otherHome(KatParameter.user.valueParam.getKey))
          .addParameter(
            Commands
              .childCommand("delete")
              .causeExtractors(KatCauseExtractor.audience)
              .permission(LibPerm.HomeOtherDelete)
              .shortDescription(Some(t"Deletes someone else's home"))
              .handle { case (ctx, (_, home, audience)) => impl.deleteHome(audience, home) }
          )
          .addParameter(
            Commands
              .childCommand("invite")
              .causeExtractors(KatCauseExtractor.audience ~ KatCauseExtractor.nameable)
              .parameters(KatParameter.player)
              .permission(LibPerm.HomeOtherDelete)
              .shortDescription(Some(t"Invite someone to someone's home"))
              .handle { case (ctx, (_, home, audience, senderNamed, player)) => 
                impl.inviteToHome(audience, senderNamed.getName, home, player.identity) 
              }
          )
          .addParameter(
            Commands
              .childCommand("residents")
              .children(
                Seq(
                  Commands
                    .childCommand("add")
                    .causeExtractors(KatCauseExtractor.audience)
                    .parameters(KatParameter.user)
                    .permission(LibPerm.HomeOtherResidentAdd)
                    .shortDescription(Some(t"Add a user as a resident to someone's home"))
                    .handleEither { case (ctx, (_, home, audience, resident)) => impl.addResident(audience, home, resident.getProfile) },
                  Commands
                    .childCommand("remove")
                    .causeExtractors(KatCauseExtractor.audience)
                    .parameters(KatParameter.user)
                    .permission(LibPerm.HomeOtherResidentRemove)
                    .shortDescription(Some(t"Remove a user as a resident from someone's home"))
                    .handleEither { case (ctx, (_, home, audience, resident)) => impl.removeResident(audience, home, resident.getProfile) },
                )
              )
              .causeExtractors(KatCauseExtractor.audience)
              .permission(LibPerm.HomeOtherResidentList)
              .shortDescription(Some(t"List the residents of a home"))
              .handle { case (ctx, (_, home, audience)) => impl.listHomeResidents(audience, home) }
          )
          .causeExtractors(KatCauseExtractor.audience ~ KatCauseExtractor.entity)
          .permission(LibPerm.HomeOtherTp)
          .shortDescription(Some(t"Teleport to one of someone else's homes"))
          .handleEither { case (ctx, (audience, teleportable, _, home)) => impl.tpHome(audience, teleportable, home) },
        Commands
          .topCommand("accept")
          .causeExtractors(KatCauseExtractor.audience ~ KatCauseExtractor.identity)
          .parameters(KatParameter.player)
          .permission(LibPerm.HomeAccept)
          .shortDescription(Some(t"Accept a home request"))
          .handleEither { case (ctx, (audience, homeOwner, requester)) => 
            impl.accept(audience, requester.identity, homeOwner, requester)
          },
        Commands
          .topCommand("goto")
          .causeExtractors(KatCauseExtractor.audience ~ KatCauseExtractor.identity ~ KatCauseExtractor.entity)
          .parameters(KatParameter.user ~ KatParameter.string)
          .permission(LibPerm.HomeGoto)
          .shortDescription(Some(t"Go to another players home if you are allowed to go there"))
          .handleEitherAsyncIO { case (ctx, (audience, requester, toTeleportable, homeOwner, homeName)) => 
            impl.goto(audience, homeOwner.getProfile, homeName, requester, toTeleportable) 
          },
        Commands
          .topCommand("residents")
          .permission(LibPerm.HomeResidentList)
          .shortDescription(Some(t"List all your residents"))
          .causeExtractors(KatCauseExtractor.audience ~ KatCauseExtractor.identity)
          .handleAsyncIO { case (ctx, (audience, homeOwner)) => 
            impl.listResidents(audience, homeOwner, isOther = false) 
          },
        //helpCommand,
        Commands
          .topCommand("reload")
          .permission(LibPerm.Reload)
          .shortDescription(Some(t"Reloads stuff"))
          .handle { case (ctx, _) =>
            impl.reload(ctx.getCause.getAudience)
          }
      )
    )
    .permission(LibPerm.HomeTp)
    .shortDescription(Some(t"Teleport to one of your homes"))
    .causeExtractors(KatCauseExtractor.audience ~ KatCauseExtractor.player)
    .handleEither { case (ctx, (audience, homeOwner, home)) => 
      impl.tpHome(audience, homeOwner, GenOtherHome.same(home, homeOwner.identity)) 
    }
end homeCommand
