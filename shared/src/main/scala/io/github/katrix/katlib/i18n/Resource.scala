package io.github.katrix.katlib.i18n

import java.util.{Locale, ResourceBundle}

import scala.collection.JavaConverters._

import org.apache.commons.lang3.text.StrSubstitutor
import org.spongepowered.api.text.Text

trait Resource {
  def getBundle(implicit locale: Locale): ResourceBundle

  def get(key: String)(implicit locale: Locale): String = getBundle.getString(key)

  def get(key: String, params: Map[String, String])(implicit locale: Locale): String = format(get(key), params)
  def get(key: String, params: (String, String)*)(implicit locale: Locale):   String = get(key, params.toMap)

  def getText(key: String)(implicit locale: Locale): Text = Text.of(get(key))

  def getText(key: String, params: Map[String, String])(implicit locale: Locale): Text = Text.of(get(key, params))
  def getText(key: String, params: (String, String)*)(implicit locale: Locale):   Text = Text.of(get(key, params.toMap))

  protected def format(str: String, params: Map[String, String]): String =
    new StrSubstitutor(params.asJava).replace(str)
}
