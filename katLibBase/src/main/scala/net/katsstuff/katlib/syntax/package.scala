package net.katsstuff.katlib

import java.util.{Locale, UUID}

import cats.FlatMap
import cats.syntax.all._
import net.katsstuff.katlib.algebras.{CommandSources, Locations, Players, Users}
import net.katsstuff.minejson.text.Text

package object syntax {

  implicit class UsersSyntax[User](private val user: User) extends AnyVal {
    def name[F[_], Player](implicit users: Users[F, User, Player]):      F[String]         = users.name(user)
    def uniqueId[F[_], Player](implicit users: Users[F, User, Player]):  UUID              = users.uniqueId(user)
    def getPlayer[F[_], Player](implicit users: Users[F, User, Player]): F[Option[Player]] = users.getPlayer(user)
    def hasPermission[F[_], Player](permission: String)(implicit users: Users[F, User, Player]): F[Boolean] =
      users.hasPermission(user, permission)
    def getOption[F[_], Player](option: String)(implicit users: Users[F, User, Player]): F[Option[String]] =
      users.getOption(user, option)
  }

  implicit class PlayersSyntax[Player](private val player: Player) extends AnyVal {
    def displayName[F[_]](implicit players: Players[F, Player]): F[Text] = players.displayName(player)
  }

  implicit class CommandSourcesSyntax[CommandSource](private val source: CommandSource) extends AnyVal {
    def name[F[_]](implicit commandSources: CommandSources[F, CommandSource]): F[String] = commandSources.name(source)
    def hasPermission[F[_]](permission: String)(implicit commandSources: CommandSources[F, CommandSource]): F[Boolean] =
      commandSources.hasPermission(source, permission)
    def sendMessage[F[_]](message: Text)(implicit commandSources: CommandSources[F, CommandSource]): F[Unit] =
      commandSources.sendMessage(source, message)
    def locale[F[_]](implicit commandSources: CommandSources[F, CommandSource]): F[Locale] =
      commandSources.locale(source)
  }

  implicit class LocationsSyntax[Location](private val location: Location) extends AnyVal {
    def x[F[_], Locatable](implicit locations: Locations[F, Location, Locatable]): F[Double] = locations.getX(location)
    def y[F[_], Locatable](implicit locations: Locations[F, Location, Locatable]): F[Double] = locations.getY(location)
    def z[F[_], Locatable](implicit locations: Locations[F, Location, Locatable]): F[Double] = locations.getZ(location)
    def yaw[F[_], Locatable](implicit locations: Locations[F, Location, Locatable]): F[Double] =
      locations.getPitch(location)
    def pitch[F[_], Locatable](implicit locations: Locations[F, Location, Locatable]): F[Double] =
      locations.getYaw(location)
    def worldId[F[_], Locatable](implicit locations: Locations[F, Location, Locatable]): F[UUID] =
      locations.getWorldId(location)

    def worldName[F[_]: FlatMap, Locatable](implicit locations: Locations[F, Location, Locatable]): F[Option[String]] =
      worldId.flatMap(locations.getWorldName)

    def makeSafe[F[_], Locatable](implicit locations: Locations[F, Location, Locatable]): F[Option[Location]] =
      locations.getSafeLocation(location)
  }

  implicit class LocatableSyntax[Locatable](private val locatable: Locatable) extends AnyVal {
    def getLocation[F[_], Location](implicit locations: Locations[F, Location, Locatable]): F[Location] =
      locations.getLocation(locatable)
  }

}
