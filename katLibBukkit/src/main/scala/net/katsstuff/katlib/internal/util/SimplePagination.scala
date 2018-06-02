package net.katsstuff.katlib.internal.util

import java.util.UUID

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

import cats.effect.Sync
import cats.syntax.all._
import net.katsstuff.katlib.helper.ProtocolHelper
import net.katsstuff.minejson.text._

case class SimplePagination(
    title: Option[Text] = None,
    header: Option[Text] = None,
    footer: Option[Text] = None,
    padding: Text = t"=",
    linesPerPage: Int = 19,
    content: Seq[Text] = Seq()
) {

  def sendTo[F[_]](
      newPages: (CommandSender, UUID => Seq[Text]) => F[Text]
  )(source: CommandSender)(implicit F: Sync[F]): F[Unit] =
    if (content.nonEmpty) {
      val paddingFiller = Text(Seq.fill(10)(padding): _*)

      def createNextButton(uuid: UUID, pageNum: Int, pageEnd: Int): Text = {
        if (pageNum == pageEnd - 1) Text.Empty
        else {
          val basic     = t">>"
          val withHover = basic.onHover = HoverAction.ShowText(t"Next page")
          withHover.onClick = ClickAction.RunCommand(s"/katlib:page next ${uuid.toString}")
        }
      }

      def createPrevButton(uuid: UUID, pageNum: Int): Text = {
        if (pageNum == 0) Text.Empty
        else {
          val basic     = t"<<"
          val withHover = basic.onHover = HoverAction.ShowText(t"Previous page")
          withHover.onClick = ClickAction.RunCommand(s"/katlib:page prev ${uuid.toString}")
        }
      }

      val rawPages = content.grouped(linesPerPage - 2).toSeq.zipWithIndex
      val createPages = (uuid: UUID) => {
        val titleLength = title.getOrElse(Text.Empty).toPlain.length
        val pageEnd     = rawPages.size
        rawPages.map {
          case (page, pageNum) =>
            val bottomText =
              t"$Yellow${createPrevButton(uuid, pageNum)} Page ${pageNum + 1} of $pageEnd ${createNextButton(uuid, pageNum, pageEnd)}"
            val bottomLength = bottomText.toPlain.length

            val extraPaddingBottom = Seq.fill((titleLength - bottomLength) / 2)(padding)
            val extraPaddingTop    = Seq.fill((bottomLength - titleLength) / 2)(padding)

            val top = Text(
              (paddingFiller +: extraPaddingTop) ++ (title
                .getOrElse(Text.Empty) +: extraPaddingTop :+ paddingFiller): _*
            )
            val bottom =
              Text((paddingFiller +: extraPaddingBottom) ++ (bottomText +: extraPaddingBottom :+ paddingFiller): _*)

            val texts = (Seq(top, header.getOrElse(Text.Empty)) ++ page ++ Seq(footer.getOrElse(Text.Empty), bottom))
              .filter(_ != Text.Empty)

            texts.reduce((t1, t2) => t"$t1\n$t2").trim
        }
      }

      newPages(source, createPages).flatMap { firstPage =>
        source match {
          case player: Player => F.delay(ProtocolHelper.sendPlayerMessage(player, firstPage))
          case _              => F.delay(source.sendMessage(firstPage.toPlain))
        }
      }
    } else F.unit
}
