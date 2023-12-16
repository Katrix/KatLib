package net.katsstuff.magicalwarps.cmd

import net.katsstuff.minejson.text.Text
import net.katsstuff.magicalwarps.Warp

case class WarpWithName[WorldId](name: String, warp: Warp[WorldId]) {

  def textName: Text = warp.textDisplayName(name)
}
case class WarpGroup(name: String)
