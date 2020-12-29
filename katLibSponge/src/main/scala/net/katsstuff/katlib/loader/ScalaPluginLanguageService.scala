package net.katsstuff.katlib.loader

import org.spongepowered.plugin.jvm.JVMPluginLanguageService

class ScalaPluginLanguageService extends JVMPluginLanguageService:

  override def getName: String = "scala_plain"

  override def getPluginLoader: String = "net.katsstuff.katlib.loader.ScalaPluginLoader"
