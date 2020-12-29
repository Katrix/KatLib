package net.katsstuff.katlib

import org.apache.logging.log4j.Logger
import org.spongepowered.api.{ResourceKey, Sponge}
import org.spongepowered.api.asset.Asset
import org.spongepowered.api.config.ConfigRoot
import org.spongepowered.plugin.PluginContainer

import java.nio.file.Path
import scala.concurrent.ExecutionContext

trait KatLibPlugin:
  def usesSharedConfig: Boolean = true
  
  given KatLibPlugin = this
  given container: PluginContainer = Sponge.getPluginManager.fromInstance(this).get
  
  lazy val configRoot: ConfigRoot = 
    val configManager = Sponge.getConfigManager
    if usesSharedConfig then 
      configManager.getSharedConfig(container)
    else 
      configManager.getPluginConfig(container)
  end configRoot

  def asset(assetId: String): Asset = 
    Sponge
      .getAssetManager
      .getAsset(container, assetId)
      .orElseThrow(() => new NoSuchElementException(s"Cannot find asset $assetId"))
      
  lazy val logger: Logger = container.getLogger

  given ExecutionContext = ExecutionContext.fromExecutor(Sponge.getAsyncScheduler.createExecutor(container))

  def resourceKey(s: String): ResourceKey = ResourceKey.of(container, s)
