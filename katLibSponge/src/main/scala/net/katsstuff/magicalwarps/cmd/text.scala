package net.katsstuff.magicalwarps.cmd

import net.katsstuff.minejson.text._

def confirmButton(label: Text, command: String): Text =
  t"[$label]".onClick(ClickAction.SuggestCommand(command))

def button(label: Text, command: String): Text =
  t"[$label]".onClick(ClickAction.RunCommand(command)).hoverText(HoverText.ShowText(t"$command"))

def bigButton(command: (Text, String)): Text =
  t"""[[${command._1}]]""".onClick(ClickAction.RunCommand(command._2)).hoverText(HoverText.ShowText(t"${command._2}"))

def bigButton4(
  command1: (Text, String),
  command2: (Text, String),
  command3: (Text, String),
  command4: (Text, String)
): Text = {

  val button1 = bigButton(command1)
  val button2 = bigButton(command2)
  val button3 = bigButton(command3)
  val button4 = bigButton(command4)

  t"""$button1 $button2 $button3 $button4"""
}
