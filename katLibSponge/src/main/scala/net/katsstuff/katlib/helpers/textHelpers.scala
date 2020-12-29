package net.katsstuff.katlib.helpers

import net.katsstuff.minejson.text.Text
import net.kyori.adventure.audience.{Audience, MessageType}
import net.kyori.adventure.identity.{Identified, Identity}
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

extension (audience: Audience) 
  def sendMessage(text: Text, source: Identity = Identity.nil(), messageType: MessageType = MessageType.SYSTEM): Unit = audience.sendMessage(text.toSponge)

extension (text: Text)
  def toSponge: Component =
    GsonComponentSerializer.gson().deserialize(text.toJson)
