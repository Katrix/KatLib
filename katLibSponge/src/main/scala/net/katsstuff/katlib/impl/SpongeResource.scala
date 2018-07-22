package net.katsstuff.katlib.impl

import java.util.{Locale, ResourceBundle}

import scala.collection.JavaConverters._
import scala.util.matching.Regex

import org.apache.commons.lang3.text.StrSubstitutor
import org.spongepowered.api.text.TextTemplate

import cats.Monad
import cats.syntax.all._
import net.katsstuff.katlib.algebras.{Resource, TextConversion}
import net.katsstuff.minejson.text.Text

class SpongeResource[F[_]: Monad](location: String)(implicit T: TextConversion[F]) extends Resource[F] {

  val findArgs: Regex = """\$\{(.+?)\}""".r

  override def getBundle(implicit locale: Locale): F[ResourceBundle] = ResourceBundle.getBundle(location, locale).pure

  override def get(key: String)(implicit locale: Locale): F[String] = getBundle.map(_.getString(key))

  override def get(key: String, params: Map[String, String])(implicit locale: Locale): F[String] =
    get(key).map(format(params))
  override def get(key: String, params: (String, String)*)(implicit locale: Locale): F[String] =
    get(key, params.toMap)

  override def getText(key: String)(implicit locale: Locale): F[Text] = get(key).map(Text.apply)

  override def getText(key: String, params: Map[String, AnyRef])(implicit locale: Locale): F[Text] =
    get(key).flatMap(formatText(params))
  override def getText(key: String, params: (String, AnyRef)*)(implicit locale: Locale): F[Text] =
    getText(key, params.toMap)

  private def format(params: Map[String, String])(str: String): String =
    new StrSubstitutor(params.asJava).replace(str)

  private def formatText(params: Map[String, _])(str: String): F[Text] = {
    if (params.forall(t => t._2.isInstanceOf[String]))
      (Text(format(params.asInstanceOf[Map[String, String]])(str)): Text).pure[F]
    else {
      val args    = findArgs.findAllIn(str).toSeq.map(s => TextTemplate.arg(s.substring(2, s.length - 1)))
      val nonArgs = findArgs.split(str).toSeq.padTo(args.length, "")

      val objs = nonArgs.zip(args).flatMap {
        case (str1, str2) =>
          if (str1.isEmpty) Seq(str2)
          else Seq(str1, str2)
      }

      val spongeText = TextTemplate.of("${", "}", objs.toArray).apply(params.asJava).build()

      T.spongeToOur(spongeText)
    }
  }
}
