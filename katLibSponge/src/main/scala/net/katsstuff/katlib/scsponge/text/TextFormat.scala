package net.katsstuff.katlib.scsponge.text

object TextFormat {

  def apply(color: TextColor = NoColor, style: TextStyle = NoStyle) = new TextFormat(color, style)
}
