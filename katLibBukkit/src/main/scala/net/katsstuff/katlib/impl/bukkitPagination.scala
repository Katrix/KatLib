package net.katsstuff.katlib.impl

import java.util.UUID

import org.bukkit.command.CommandSender

import cats.effect.Sync
import net.katsstuff.katlib.algebras.{PageOperations, Pagination}
import net.katsstuff.katlib.internal.util.SimplePagination
import net.katsstuff.minejson.text.Text

class BukkitPagination[F[_]](newPages: (CommandSender, UUID => Seq[Text]) => F[Text])(implicit F: Sync[F])
    extends Pagination[F, CommandSender] {
  override type Page = List[PageOps]
  override val pageOperations: PageOperations[List[PageOps]] = PageOps

  override def sendPage(page: List[PageOps], source: CommandSender): F[Unit] = {
    import PageOps._

    page
      .foldLeft(SimplePagination()) {
        case (builder, SetTitle(title))               => builder.copy(title = Some(title))
        case (builder, SetHeader(header))             => builder.copy(header = Some(header))
        case (builder, SetFooter(footer))             => builder.copy(footer = Some(footer))
        case (builder, SetPadding(padding))           => builder.copy(padding = padding)
        case (builder, SetLinesPerPage(linesPerPage)) => builder.copy(linesPerPage = linesPerPage)
        case (builder, SetContent(content))           => builder.copy(content = content)
      }
      .sendTo(newPages)(source)
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
