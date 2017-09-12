package io.github.katrix.katlib.i18n

import java.util.{Locale, ResourceBundle}

import scala.collection.JavaConverters._
import scala.util.matching.Regex

import org.apache.commons.lang3.text.StrSubstitutor
import org.spongepowered.api.text.{Text, TextTemplate}

trait Resource {
  val findArgs: Regex = """\$\{(.+?)\}""".r

  def getBundle(implicit locale: Locale): ResourceBundle

  def get(key: String)(implicit locale: Locale): String = getBundle.getString(key)

  def get(key: String, params: Map[String, String])(implicit locale: Locale): String = format(get(key), params)
  def get(key: String, params: (String, String)*)(implicit locale: Locale):   String = get(key, params.toMap)

  def getText(key: String)(implicit locale: Locale): Text = Text.of(get(key))

  def getText(key: String, params: Map[String, AnyRef])(implicit locale: Locale): Text = formatText(get(key), params)
  def getText(key: String, params: (String, AnyRef)*)(implicit locale: Locale):   Text = getText(key, params.toMap)

  private def format(str: String, params: Map[String, String]): String =
    new StrSubstitutor(params.asJava).replace(str)

  private def formatText(str: String, params: Map[String, _]): Text = {
    if (params.forall(t => t._2.isInstanceOf[String])) Text.of(format(str, params.asInstanceOf[Map[String, String]]))
    else {
      val args    = findArgs.findAllIn(str).toSeq.map(s => TextTemplate.arg(s.substring(2, s.length - 1)))
      val nonArgs = findArgs.split(str).toSeq.padTo(args.length, "")

      val objs = nonArgs.zip(args).flatMap {
        case (str1, str2) =>
          if (str1.isEmpty) Seq(str2)
          else Seq(str1, str2)
      }

      TextTemplate.of("${", "}", objs.toArray).apply(params.asJava).build()
    }
  }
}
