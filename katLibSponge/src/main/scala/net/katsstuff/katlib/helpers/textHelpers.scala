package net.katsstuff.katlib.helpers

import scala.jdk.CollectionConverters._
import net.katsstuff.minejson.text.Text
import net.katsstuff.minejson.text.serializer.JsonTextSerializer
import net.kyori.adventure.audience.{Audience, MessageType}
import net.kyori.adventure.identity.{Identified, Identity}
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.spongepowered.api.command.CommandCause
import org.spongepowered.api.command.parameter.CommandContext
import org.spongepowered.api.service.pagination.PaginationList

extension (audience: Audience) 
  def sendMessage(text: Text, source: Identity = Identity.nil(), messageType: MessageType = MessageType.SYSTEM): Unit = 
    audience.sendMessage(text.toSponge)

extension (cmdCtx: CommandContext)
  def sendMessageCtx(text: Text, source: Identity = Identity.nil(), messageType: MessageType = MessageType.SYSTEM): Unit =
    cmdCtx.getCause.sendMessageCause(text, source, messageType)

extension (cause: CommandCause)
  def sendMessageCause(text: Text, source: Identity = Identity.nil(), messageType: MessageType = MessageType.SYSTEM): Unit =
    cause.getAudience.sendMessage(text, source, messageType)

extension (text: Text)
  def toSponge: Component =
    GsonComponentSerializer.gson().deserialize(text.toJson)

extension (component: Component)
  def toKatText: Text =
    JsonTextSerializer.deserializeThrow(GsonComponentSerializer.gson().serialize(component))

extension (builder: PaginationList.Builder)
  def contents(contents: Text*): PaginationList.Builder = builder.contents(contents.map(_.toSponge): _*)
  
  def contents(contents: Iterable[Text]): PaginationList.Builder = builder.contents(contents.map(_.toSponge).asJava)
  
  def title(title: Text): PaginationList.Builder = builder.title(title.toSponge)
  def noTitle: PaginationList.Builder            = builder.title(null)

  def header(header: Text): PaginationList.Builder = builder.header(header.toSponge)
  def noHeader: PaginationList.Builder             = builder.header(null)

  def footer(footer: Text): PaginationList.Builder = builder.footer(footer.toSponge)
  def noFooter: PaginationList.Builder             = builder.footer(null)

  def padding(padding: Text): PaginationList.Builder = builder.padding(padding.toSponge)
