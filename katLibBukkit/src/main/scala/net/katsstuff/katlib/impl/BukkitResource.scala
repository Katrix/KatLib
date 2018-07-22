package net.katsstuff.katlib.impl

import java.util.{Locale, ResourceBundle}

import scala.collection.JavaConverters._
import scala.util.matching.Regex

import org.apache.commons.lang.text.StrSubstitutor

import cats.Monad
import cats.syntax.all._
import net.katsstuff.katlib.algebras.Resource
import net.katsstuff.minejson.text.Text

class BukkitResource[F[_]: Monad](location: String) extends Resource[F] {

  val findArgs: Regex = """\$\{(.+?)\}""".r

  override def getBundle(implicit locale: Locale): F[ResourceBundle] = ResourceBundle.getBundle(location, locale).pure

  override def get(key: String)(implicit locale: Locale): F[String] = getBundle.map(_.getString(key))

  override def get(key: String, params: Map[String, String])(implicit locale: Locale): F[String] =
    get(key).map(format(params))
  override def get(key: String, params: (String, String)*)(implicit locale: Locale): F[String] =
    get(key, params.toMap)

  override def getText(key: String)(implicit locale: Locale): F[Text] = get(key).map(Text.apply)

  override def getText(key: String, params: Map[String, AnyRef])(implicit locale: Locale): F[Text] =
    get(key).map(formatText(params))
  override def getText(key: String, params: (String, AnyRef)*)(implicit locale: Locale): F[Text] =
    getText(key, params.toMap)

  private def format(params: Map[String, String])(str: String): String =
    new StrSubstitutor(params.asJava).replace(str)

  private def formatText(params: Map[String, _])(str: String): Text = {
    if (params.forall(t => t._2.isInstanceOf[String]))
      Text(format(params.asInstanceOf[Map[String, String]])(str))
    else {
      val args: Seq[AnyRef] =
        findArgs.findAllIn(str).toSeq.map(s => params.mapValues(_.asInstanceOf[AnyRef]).getOrElse(s, s: AnyRef))
      val nonArgs: Seq[String] = findArgs.split(str).toSeq.padTo(args.length, "")

      val objs = nonArgs.zip(args).flatMap {
        case (str1, str2) =>
          if (str1.isEmpty) Seq(str2)
          else Seq(str1, str2)
      }

      Text.apply(objs: _*)
    }
  }
}
