package net.katsstuff.magicalwarps.cmd

import net.katsstuff.katlib.spongecommands.{Commands, KatCauseExtractor, KatParameter}
import net.katsstuff.katlib.helpers._
import net.katsstuff.minejson.text._
import net.katsstuff.magicalwarps.{Warp, WarpHandler}
import net.katsstuff.magicalwarps.lib.LibPerm
import org.spongepowered.api.ResourceKey
import cats.effect._
import org.spongepowered.api.command.{CommandResult, CommandResults}
import org.spongepowered.api.entity.Entity
import org.spongepowered.api.world.server.ServerLocation
import org.spongepowered.math.vector.Vector3d

type SpongeWarpCmdImpl = WarpCmdsImpl[CommandResult, ServerLocation, Vector3d, Entity, ResourceKey]

def warpParam(using WarpHandler[ResourceKey]): KatParameter[IO[Option[WarpWithName[ResourceKey]]]] =
  ???
  
def warpGroupParam(using WarpHandler[ResourceKey]): KatParameter[WarpGroup] =
  ???

def warpsCommand(using warpHandler: WarpHandler[ResourceKey], impl: SpongeWarpCmdImpl) =
  Commands
    .topCommand("warps")
    .children(
      Seq(
        Commands
          .topCommand("set")
          .permission(LibPerm.Set)
          .shortDescription(Some(t"Set a new warp"))
          .causeExtractors(KatCauseExtractor.audience ~ KatCauseExtractor.location ~ KatCauseExtractor.rotation)
          .parameters(KatParameter.string ~ KatParameter.string.optionalConsumeAllRemaining)
          .handle { case (ctx, (audience, location, rotation, warpName, groups)) =>
            impl.setWarp(audience, warpName, groups, location, rotation)
          }
      )
    )