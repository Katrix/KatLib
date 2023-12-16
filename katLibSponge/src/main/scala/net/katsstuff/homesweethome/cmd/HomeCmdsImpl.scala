package net.katsstuff.homesweethome.cmd

import net.katsstuff.homesweethome.{Home, HomeHandler}
import net.katsstuff.homesweethome.lib.LibPerm
import net.katsstuff.katlib.spongecommands.{Commands, KatCauseExtractor, KatParameter, ParentParams}
import net.katsstuff.katlib.helpers._
import net.katsstuff.minejson.text._
import net.kyori.adventure.audience.Audience

import java.util.{Locale, UUID}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import cats.effect.IO
import net.kyori.adventure.identity.Identity

trait HomeCmdsImpl[
  CommandResult, 
  Subject,
  Location,
  Rotation,
  Teleportable,
  WorldId
](using homeHandler: HomeHandler[Subject, WorldId]):
  
  type ImplHome = Home[WorldId]
  type OtherHome = GenOtherHome[WorldId]
  
  val CmdSuccess: CommandResult

  def teleportError(homeName: String) = Left(s"""Couldn't teleport you to "$homeName". Does the world still exist?""")
  
  def sendPagination(
      title: Text,
      content: Seq[Text],
      sendTo: Audience
  ): Unit

  def makeHome(location: Location, rotation: Rotation): ImplHome
  
  def nameFromUUID(uuid: UUID): String
  
  def nameFromId(id: Identity): String = nameFromUUID(id.uuid)
  
  def subjectFromId(identity: Identity): Subject
  
  def audienceFromId(identity: Identity): Audience
  
  def isIdPresent(identity: Identity): Boolean
  
  def teleport(home: ImplHome, teleportable: Teleportable): Boolean
  
  def reloadData(): Either[Throwable, Unit]
  
  extension (otherHome: OtherHome) def homeName: String =
    if otherHome.isOther then 
      s""""${otherHome.namedHome.name}" for ${nameFromUUID(otherHome.homeOwner.uuid)}""" 
    else s""""${otherHome.namedHome.name}""""
  
  def listHomes(audience: Audience, owner: Identity, isOther: Boolean): IO[CommandResult] =
    for
      allHomes <- homeHandler.allHomesForIdent(owner)
    yield
      val homes     = allHomes.keys.toSeq
      val limit     = homeHandler.getHomeLimit(subjectFromId(owner))
      val ownerName = nameFromId(owner)
  
      if homes.isEmpty then
        val noHomesMessage = 
          if isOther then
            t"${Yellow}Limit: $limit\n$ownerName don't have any homes."
          else
            t"${Yellow}Limit: $limit\nYou don't have any homes."
        
        audience.sendMessage(noHomesMessage)
      else
        val commandPrefix  = if (isOther) s"/home other $ownerName" else "/home"
        val homeText = homes.sorted.map { rawHomeName =>
          val homeName = rawHomeName.replace("""\""", """\\""")
  
          val teleportButton = button(t"${Yellow}Teleport", s"""$commandPrefix "$homeName"""")
          val setButton      = confirmButton(t"${Yellow}Set", s"""$commandPrefix set "$homeName"""")
          val inviteButton   = confirmButton(t"${Yellow}Invite", s"""$commandPrefix "$homeName" invite <player>""")
          val deleteButton   = confirmButton(t"${Red}Delete", s"""$commandPrefix "$homeName" delete""")
  
          val residentsButton = button(t"${Yellow}Residents", s"""$commandPrefix "$homeName" residents""")
  
          t""""$homeName" $teleportButton $setButton $inviteButton $residentsButton $deleteButton"""
        }
  
        val limitText = t"Limit: $limit"
        val newButton = confirmButton(t"${Yellow}New home", s"$commandPrefix set <homeName>")
        
        sendPagination(t"${Yellow}Homes", limitText +: newButton +: homeText, audience)
      end if
  
      CmdSuccess
  
  val DisallowedHomeNames = Seq(
    "set",
    "list",
    "accept",
    "other",
    "goto",
    "residents"
  )
  
  def setHome(
    audience: Audience,
    location: Location,
    rotation: Rotation,
    owner: Identity,
    name: String,
    isOther: Boolean
  ): IO[Either[String, CommandResult]] =
    if !DisallowedHomeNames.exists(name.toLowerCase(Locale.ROOT).startsWith) then
      for
        replace  <- homeHandler.homeExist(owner, name)
        allHomes <- homeHandler.allHomesForIdent(owner)
      yield
        val limit            = homeHandler.getHomeLimit(subjectFromId(owner))
        val limitWithReplace = if (replace) limit + 1 else limit
        val limitNotReached  = allHomes.size < limitWithReplace
  
        if limitNotReached then
          homeHandler.updateHome(owner, name, makeHome(location, rotation))
  
          val homeNameText =
            if (isOther) s""""$name" for ${nameFromId(owner)}"""
            else s""""$name""""
  
          audience.sendMessage(t"${Green}Set $homeNameText successfully")
          Right(CmdSuccess)
        else 
          Left("Home limit reached")
    else
      IO.pure(Left("That home name is disallowed"))
  
  def tpHome(audience: Audience, teleportable: Teleportable, home: OtherHome): Either[String, CommandResult] =
    if teleport(home.home, teleportable) then
      audience.sendMessage(t"${Green}Teleported to ${home.homeName} successfully")
      Right(CmdSuccess)
    else 
      teleportError(home.homeName)
  
  def deleteHome(audience: Audience, home: OtherHome): CommandResult =
    homeHandler.deleteHome(home.homeOwner, home.namedHome.name)
  
    audience.sendMessage(t"${Green}Deleted ${home.homeName}")
    CmdSuccess
  
  def inviteToHome(audience: Audience, senderName: String, home: OtherHome, target: Identity): CommandResult = {
    homeHandler.addInvite(target, home.homeOwner, home.home)
    val gotoButton =
      button(t"${Yellow}Go to ${home.homeName}", s"/home goto ${nameFromId(home.homeOwner)} ${home.namedHome.name}")
    audience.sendMessage(t"${Green}Invited ${nameFromId(target)} to ${home.homeName}")
    audienceFromId(target).sendMessage(
      t"${Yellow}You have been invited to ${home.homeName} by $senderName${Text.NewLine}$Reset$gotoButton"
    )
    CmdSuccess
  }
  
  def accept(
    audience: Audience,
    requester: Identity,
    homeOwner: Identity,
    requesterTeleportable: Teleportable
  ): Either[String, CommandResult] =
    homeHandler
      .getRequest(requester, homeOwner)
      .toRight("That player has not sent a home request")
      .flatMap { home =>
        if teleport(home, requesterTeleportable) then
          audienceFromId(requester).sendMessage(t"${Yellow}Teleported you to your requested home")
          audience.sendMessage(t"${Green}Teleported ${nameFromId(requester)} to their requested home")
          homeHandler.removeRequest(requester, homeOwner)
          Right(CmdSuccess)
        else 
          teleportError("your requested home")
      }
  
  def goto(
    audience: Audience,
    homeOwner: Identity,
    homeName: String,
    requester: Identity,
    requesterTeleportable: Teleportable
  ): IO[Either[String, CommandResult]] =
    for
      homeOpt <- homeHandler.getHome(homeOwner, homeName)
    yield
      homeOpt.toRight(HomeNotFound).flatMap { home =>
        val isResident    = home.residents.contains(requester.uuid)
        val isInvited     = homeHandler.isInvited(requester, homeOwner, home) && isIdPresent(homeOwner)
        val canUseGoto    = isResident || isInvited
        val requesterName = nameFromId(requester)
        val homeOwnerName = nameFromId(homeOwner)

        if (canUseGoto) {
          if (teleport(home, requesterTeleportable)) {
            audience.sendMessage(t"""${Green}Teleported to "$homeName" for $requesterName""")
            homeHandler.removeInvite(requester, homeOwner)
            Right(CmdSuccess)
          } else {
            teleportError(s""""$homeName" for $homeOwnerName""")
          }
        } else {
          if (isIdPresent(homeOwner)) {
            homeHandler.addRequest(requester, homeOwner, home)
            audience.sendMessage(t"""${Green}Sent home request to $homeOwnerName for "$homeName"""")
            val acceptButton = button(t"${Yellow}Accept", s"/home accept $requesterName")
            audienceFromId(homeOwner).sendMessage(
              t"""$Yellow$requesterName has requested a to be teleported to "$homeName".${Text.NewLine}$Reset$acceptButton"""
            )
            Right(CmdSuccess)
          } else {
            Left("The player you tried to send a home request to is offline")
          }
        }
      }
  
  def listHomeResidents(audience: Audience, home: OtherHome): CommandResult = 
    val otherPrefix = if (home.isOther) s"/home other ${nameFromId(home.homeOwner)}" else "/home"
    val limit       = homeHandler.getResidentLimit(subjectFromId(home.homeOwner))
  
    val homeName  = home.namedHome.name.replace("""\""", """\\""")
    val residents = home.home.residents.toSeq
    val title     = t"""$Yellow"$homeName"'s residents"""
  
    val residentText =
      if residents.isEmpty then Seq(t"${Yellow}No residents")
      else
        residents
          .map(nameFromUUID)
          .sorted
          .map { residentName =>
            val deleteButton =
              confirmButton(
                t"${Red}Delete",
                s"""$otherPrefix residents remove "$homeName" $residentName"""
              )

            t"$residentName $deleteButton"
          }
  
    val limitText = t"Limit: $limit"
    val newButton = confirmButton(t"${Yellow}New resident", s"""$otherPrefix residents add "$homeName" <player>""")
    
    sendPagination(title, limitText +: newButton +: residentText, audience)
    CmdSuccess
  
  def addResident(audience: Audience, home: OtherHome, resident: Identity): Either[String, CommandResult] = {
    val limitNotReached = home.home.residents.size < homeHandler.getResidentLimit(subjectFromId(home.homeOwner))
  
    if limitNotReached then
      if !home.home.residents.contains(resident.uuid) then
        val newHome = home.home.addResident(resident.uuid)
        homeHandler.updateHome(home.homeOwner, home.namedHome.name, newHome)
  
        audience.sendMessage(t"${Green}Adding ${nameFromId(resident)} as a resident to ${home.homeName}")
        Right(CmdSuccess)
      else 
        Left(s"${nameFromId(resident)} is already a resident of ${home.homeName}")
    else 
      Left("Resident limit reached")
  }
  
  def removeResident(audience: Audience, home: OtherHome, resident: Identity): Either[String, CommandResult] =
    if home.home.residents.contains(resident.uuid) then 
      val newHome = home.home.removeResident(resident.uuid)
      homeHandler.updateHome(home.homeOwner, home.namedHome.name, newHome)
  
      audience.sendMessage(t"${Green}Removed ${nameFromId(resident)} as a resident from ${home.homeName}")
      Right(CmdSuccess)
    else 
      Left(s"""${nameFromId(resident)} is not a resident of ${home.homeName}""")
  
  def listResidents(audience: Audience, owner: Identity, isOther: Boolean): IO[CommandResult] =
    val ownerName   = nameFromId(owner)
    val otherPrefix = if (isOther) s"/home other $ownerName" else "/home"
    val limit       = homeHandler.getResidentLimit(subjectFromId(owner))
  
    for
      homes <- homeHandler.allHomesForIdent(owner)
    yield
      val residents = homes.map((name, home) => name -> home.residents)
      val title     = t"""$Yellow$ownerName's residents"""
  
      val residentText =
        if (residents.isEmpty) Seq(t"${Yellow}No homes")
        else
          residents.toSeq
            .sortBy(_._1)
            .map {
              case (homeName, homeResidentsUuids) =>
                val details = button(t"${Yellow}Details", s"""${otherPrefix} residents "${homeName.replace("""\""", """\\""")}"""")
                if homeResidentsUuids.isEmpty then 
                  t""""$homeName": ${Yellow}No residents$Reset $details"""
                else
                  val homeResidents = homeResidentsUuids.map(nameFromUUID)
                  t""""$homeName": $Yellow${homeResidents.mkString(", ")}$Reset $details"""
            }
  
      val limitText = t"Limit: $limit"
        
      sendPagination(title, limitText +: residentText, audience)
      CmdSuccess
  
  def reload(audience: Audience)(using ExecutionContext): CommandResult =
    Future {
      reloadData() match {
        case Right(()) =>
          audience.sendMessage(t"${Green}Reload success")
        case Left(e) =>
          audience.sendMessage(t"$Red${e.getMessage}")
      }
    }

    CmdSuccess

end HomeCmdsImpl
