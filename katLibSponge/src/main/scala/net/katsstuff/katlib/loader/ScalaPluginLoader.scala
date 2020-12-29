package net.katsstuff.katlib.loader

import org.spongepowered.plugin.{InvalidPluginException, PluginCandidate, PluginEnvironment}
import org.spongepowered.plugin.jvm.locator.JVMPluginResource
import org.spongepowered.plugin.jvm.{JVMPluginContainer, JVMPluginLoader}

import java.util.Optional
import scala.util.control.NonFatal

class ScalaPluginLoader extends JVMPluginLoader[JVMPluginContainer]:

  override def createPluginContainer(
    candidate: PluginCandidate[JVMPluginResource], 
    environment: PluginEnvironment
  ): Optional[JVMPluginContainer] = Optional.of(new JVMPluginContainer(candidate))

  override def createPluginInstance(
    environment: PluginEnvironment, 
    container: JVMPluginContainer, 
    targetClassLoader: ClassLoader
  ): AnyRef = 
    try 
      val mainClass = container.getMetadata.getMainClass
      val objectClass = if(mainClass.endsWith("$")) mainClass else mainClass + "$"
      val module = Class.forName(objectClass, true, targetClassLoader)
      module.getField("MODULE$").get(null)
    catch 
      case NonFatal(e) => 
        throw new InvalidPluginException(s"An error occurred creating an instance of plugin '${container.getMetadata.getId}'", e)