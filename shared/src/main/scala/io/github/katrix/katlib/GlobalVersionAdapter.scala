package io.github.katrix.katlib

import org.spongepowered.api.service.pagination.PaginationList
import org.spongepowered.api.text.channel.MessageReceiver

/**
  * A class which represents methods which differs by API.
  */
trait GlobalVersionAdapter {

  def sendPagination(paginationList: PaginationList.Builder, receiver: MessageReceiver): Unit

}
