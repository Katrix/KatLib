package net.katsstuff.katlib.impl

import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.service.pagination.PaginationList

import cats.effect.Sync
import cats.syntax.all._
import net.katsstuff.katlib.algebras.{PageOperations, Pagination, TextConversion}
import net.katsstuff.minejson.text.Text

class SpongePagination[F[_]](implicit F: Sync[F], T: TextConversion[F]) extends Pagination[F, CommandSource] {
  override type Page = List[PageOps]
  override val pageOperations: PageOperations[List[PageOps]] = PageOps
  override def sendPage(page: List[PageOps], source: CommandSource): F[Unit] = {
    import PageOps._

    import cats.instances.list._

    page
      .foldLeftM(PaginationList.builder()) {
        case (builder, SetTitle(title))               => T.ourToSponge(title).flatMap(t => F.delay(builder.title(t)))
        case (builder, SetHeader(header))             => T.ourToSponge(header).flatMap(t => F.delay(builder.header(t)))
        case (builder, SetFooter(footer))             => T.ourToSponge(footer).flatMap(t => F.delay(builder.footer(t)))
        case (builder, SetPadding(padding))           => T.ourToSponge(padding).flatMap(t => F.delay(builder.padding(t)))
        case (builder, SetLinesPerPage(linesPerPage)) => F.delay(builder.linesPerPage(linesPerPage))
        case (builder, SetContent(content)) =>
          content.map(T.ourToSponge).toList.sequence.flatMap(ts => F.delay(builder.contents(ts: _*)))
      }
      .flatMap(builder => F.delay(builder.sendTo(source)))
  }
}

sealed trait PageOps
object PageOps extends PageOperations[List[PageOps]] {

  case class SetTitle(title: Text)              extends PageOps
  case class SetHeader(header: Text)            extends PageOps
  case class SetFooter(footer: Text)            extends PageOps
  case class SetPadding(padding: Text)          extends PageOps
  case class SetLinesPerPage(linesPerPage: Int) extends PageOps
  case class SetContent(content: Seq[Text])     extends PageOps

  override def setTitle(title: Text):              List[PageOps] = List(SetTitle(title))
  override def setHeader(header: Text):            List[PageOps] = List(SetHeader(header))
  override def setFooter(footer: Text):            List[PageOps] = List(SetFooter(footer))
  override def setPadding(padding: Text):          List[PageOps] = List(SetPadding(padding))
  override def setLinesPerPage(linesPerPage: Int): List[PageOps] = List(SetLinesPerPage(linesPerPage))
  override def setContent(content: Seq[Text]):     List[PageOps] = List(SetContent(content))
}
