package io.github.katrix.katlib

import org.spongepowered.api.service.pagination.PaginationList
import org.spongepowered.api.text.channel.MessageReceiver

trait KatPlugin700 extends KatPlugin {

  override val globalVersionAdapter: GlobalVersionAdapter = new GlobalVersionAdapter {
    override def sendPagination(paginationList: PaginationList.Builder, receiver: MessageReceiver): Unit =
      paginationList.sendTo(receiver)
  }
}
