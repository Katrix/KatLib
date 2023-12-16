package net.katsstuff.homesweethome.cmd

import net.katsstuff.minejson.text._

def confirmButton(button: Text, text: String): Text =
  t"[$button]".onClick(ClickAction.SuggestCommand(text))

def button(button: Text, text: String): Text =
  t"[$button]".onClick(ClickAction.RunCommand(text)).hoverText(HoverText.ShowText(t"$text"))

final val HomeNotFound = "No home with that name found"
