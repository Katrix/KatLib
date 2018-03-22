package net.katsstuff.katlib

import java.io.{BufferedReader, BufferedWriter, ByteArrayInputStream, ByteArrayOutputStream, StringReader, StringWriter}

import org.spongepowered.api.data.DataView
import org.spongepowered.api.data.persistence.DataFormats

import ninja.leaping.configurate.gson.GsonConfigurationLoader
import ninja.leaping.configurate.hocon.HoconConfigurationLoader

object KatLibDataFormats {

  def readJson(string: String): DataView = {
    val writer = new BufferedWriter(new StringWriter())
    val gsonLoader = GsonConfigurationLoader
      .builder()
      .setSource(() => new BufferedReader(new StringReader(string)))
      .build()

    val hoconLoader = HoconConfigurationLoader
      .builder()
      .setSink(() => writer)
      .build()

    hoconLoader.save(gsonLoader.load())

    DataFormats.HOCON.readFrom(new ByteArrayInputStream(writer.toString.getBytes("UTF-8")))
  }

  def writeJson(view: DataView): String = {
    val baos = new ByteArrayOutputStream()
    DataFormats.HOCON.writeTo(baos, view)
    val hoconStr = new String(baos.toByteArray, "UTF-8")

    val writer = new BufferedWriter(new StringWriter())
    val hoconLoader = HoconConfigurationLoader
      .builder()
      .setSource(() => new BufferedReader(new StringReader(hoconStr)))
      .build()

    val gsonLoader = GsonConfigurationLoader
      .builder()
      .setSink(() => writer)
      .build()

    gsonLoader.save(hoconLoader.load())

    writer.toString
  }
}
