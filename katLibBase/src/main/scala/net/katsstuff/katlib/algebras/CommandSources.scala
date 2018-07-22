package net.katsstuff.katlib.algebras

import java.util.Locale

import net.katsstuff.minejson.text.Text

/**
  * Provides access to some properties and actions to execute on a command source.
  */
trait CommandSources[F[_], CommandSource] {

  /**
    * Returns the name of a command source.
    */
  def name(source: CommandSource): F[String]

  /**
    * Checks if a command source has a permission.
    */
  def hasPermission(source: CommandSource, permission: String): F[Boolean]

  /**
    * Sends a message to a command source.
    */
  def sendMessage(source: CommandSource, message: Text): F[Unit]

  /**
    * Returns the locale of a command source.
    */
  def locale(source: CommandSource): F[Locale]
}
