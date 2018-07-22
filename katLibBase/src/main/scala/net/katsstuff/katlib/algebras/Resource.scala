package net.katsstuff.katlib.algebras

import java.util.{Locale, ResourceBundle}

import net.katsstuff.minejson.text.Text

/**
  * Provides functional access to a [[ResourceBundle]] and some methods to
  * get stuff from it.
  */
trait Resource[F[_]] {

  /**
    * Gets the bundle this Resource represents.
    */
  def getBundle(implicit locale: Locale): F[ResourceBundle]

  /**
    * Get a string for a key from this resource.
    */
  def get(key: String)(implicit locale: Locale): F[String]

  /**
    * Get a string for a key from this resource, and formats it using some parameters.
    */
  def get(key: String, params: Map[String, String])(implicit locale: Locale): F[String]

  /**
    * Get a string for a key from this resource, and formats it using some parameters.
    */
  def get(key: String, params: (String, String)*)(implicit locale: Locale): F[String]

  /**
    * Get a text for a key from this resource.
    */
  def getText(key: String)(implicit locale: Locale): F[Text]

  /**
    * Get a text for a key from this resource, and formats it using some parameters.
    * The parameters themselves can be text elements.
    */
  def getText(key: String, params: Map[String, AnyRef])(implicit locale: Locale): F[Text]

  /**
    * Get a text for a key from this resource, and formats it using some parameters.
    * The parameters themselves can be text elements.
    */
  def getText(key: String, params: (String, AnyRef)*)(implicit locale: Locale): F[Text]
}
