package net.katsstuff.katlib.scsponge

import scala.annotation.tailrec

import org.spongepowered.api.text.{action, format, Text, TextTemplate}

package object text {

  type ClickAction[A]      = action.ClickAction[A]
  type HoverAction[A]      = action.HoverAction[A]
  type ShiftClickAction[A] = action.ShiftClickAction[A]

  type TextFormat = format.TextFormat

  //Styles

  type TextStyle = format.TextStyle

  val NoStyle:       TextStyle = format.TextStyles.NONE
  val Bold:          TextStyle = format.TextStyles.BOLD
  val Underlined:    TextStyle = format.TextStyles.UNDERLINE
  val Italic:        TextStyle = format.TextStyles.ITALIC
  val StrikeThrough: TextStyle = format.TextStyles.STRIKETHROUGH
  val Obfuscated:    TextStyle = format.TextStyles.OBFUSCATED

  //Colors

  type TextColor = format.TextColor

  val NoColor:     TextColor = format.TextColors.NONE
  val Black:       TextColor = format.TextColors.BLACK
  val DarkBlue:    TextColor = format.TextColors.DARK_BLUE
  val DarkGreen:   TextColor = format.TextColors.DARK_GREEN
  val DarkAqua:    TextColor = format.TextColors.DARK_AQUA
  val DarkRed:     TextColor = format.TextColors.DARK_RED
  val DarkPurple:  TextColor = format.TextColors.DARK_PURPLE
  val Gold:        TextColor = format.TextColors.GOLD
  val Gray:        TextColor = format.TextColors.GRAY
  val DarkGray:    TextColor = format.TextColors.DARK_GRAY
  val Blue:        TextColor = format.TextColors.BLUE
  val Green:       TextColor = format.TextColors.GREEN
  val Aqua:        TextColor = format.TextColors.AQUA
  val Red:         TextColor = format.TextColors.RED
  val LightPurple: TextColor = format.TextColors.LIGHT_PURPLE
  val Yellow:      TextColor = format.TextColors.YELLOW
  val White:       TextColor = format.TextColors.WHITE
  val Reset:       TextColor = format.TextColors.RESET

  implicit class TextSyntax(private val sc: StringContext) extends AnyVal {

    /**
      * Create a [[Text]] representation of this string.
      * Really just a nicer way of saying [[Text#of(anyRef: AnyRef*]]
      */
    def t(args: Any*): Text = {
      sc.checkLengths(args)

      @tailrec
      def inner(partsLeft: Seq[String], argsLeft: Seq[Any], res: Seq[AnyRef]): Seq[AnyRef] =
        if (argsLeft == Nil) res
        else {
          inner(partsLeft.tail, argsLeft.tail, (res :+ argsLeft.head.asInstanceOf[AnyRef]) :+ partsLeft.head)
        }

      Text.of(inner(sc.parts.tail, args, Seq(sc.parts.head)): _*)
    }

    /**
      * Create a [[Text]] representation of this string.
      * String arguments are converted into [[TextTemplate.Arg]]s
      * Really just a nicer way of saying [[TextTemplate#of(anyRef: AnyRef*]]
      */
    def tt(args: Any*): TextTemplate = {
      sc.checkLengths(args)

      @tailrec
      def inner(partsLeft: Seq[String], argsLeft: Seq[Any], res: Seq[AnyRef]): Seq[AnyRef] =
        if (argsLeft == Nil) res
        else {
          val argObj = argsLeft.head match {
            case string: String => TextTemplate.arg(string)
            case any @ _        => any.asInstanceOf[AnyRef]
          }
          inner(partsLeft.tail, argsLeft.tail, (res :+ argObj) :+ partsLeft.head)
        }

      TextTemplate.of(inner(sc.parts.tail, args, Seq(sc.parts.head)): _*)
    }
  }
}
