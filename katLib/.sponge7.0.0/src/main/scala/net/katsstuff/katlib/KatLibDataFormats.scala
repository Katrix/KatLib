package net.katsstuff.katlib

import org.spongepowered.api.data.DataView
import org.spongepowered.api.data.persistence.DataFormats

object KatLibDataFormats {

  def readJson(string: String):  DataView = DataFormats.JSON.read(string)
  def writeJson(view: DataView): String   = DataFormats.JSON.write(view)
}
