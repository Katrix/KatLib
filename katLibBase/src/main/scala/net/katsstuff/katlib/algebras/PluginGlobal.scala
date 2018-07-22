package net.katsstuff.katlib.algebras

import java.nio.file.Path

/**
  * Contains global constants and info releated to the plugin.
  */
trait PluginGlobal[F[_]] {

  /**
    * The plugin id.
    */
  def id: String

  /**
    * The plugin name.
    */
  def name: String

  /**
    * The plugin name.
    */
  def version: String

  /**
    * The plugin description.
    */
  def description: String

  /**
    * The place where config files and such are stored.
    */
  def pluginDirectory: Path

  /**
    * Shifts execution to the main thread.
    */
  def shiftSync: F[Unit]

  /**
    * Shifts execution away from the main thread.
    */
  def shiftAsync: F[Unit]

}
