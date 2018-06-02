package net.katsstuff.katlib.helper

import scala.util.Try

import org.bukkit.entity.Player

import com.comphenix.protocol.wrappers.WrappedChatComponent
import com.comphenix.protocol.{PacketType, ProtocolLibrary}

import net.katsstuff.minejson.text.Text

object ProtocolHelper {

  private val protocolManager = ProtocolLibrary.getProtocolManager

  def sendPlayerMessage(player: Player, text: Text): Unit = {
    val packet          = protocolManager.createPacket(PacketType.Play.Server.CHAT)
    val chatComponent   = WrappedChatComponent.fromJson(text.optimize.toJson)
    packet.getChatComponents.write(0, chatComponent)

    Try(protocolManager.sendServerPacket(player, packet)).failed.foreach { e =>
      e.printStackTrace() //TODO: Handle this better
    }
  }

}
