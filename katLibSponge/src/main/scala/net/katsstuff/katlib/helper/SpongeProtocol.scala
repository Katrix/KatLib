package net.katsstuff.katlib.helper

import java.net.{URI, URL}
import java.util.UUID

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.util.Try

import org.spongepowered.api.data.DataSerializable
import org.spongepowered.api.text.format.{TextColor, TextColors, TextFormat, TextStyle, TextStyles}
import org.spongepowered.api.text.{LiteralText, Text, TextTemplate}
import org.spongepowered.api.{CatalogType, Sponge}

import io.circe._
import io.circe.syntax._
import Implicits._
import metaconfig._
import net.katsstuff.katlib.KatLibDataFormats

trait SpongeProtocol {

  implicit val confEncoder: Encoder[Conf] = Encoder.instance {
    case Conf.Null()        => Json.Null
    case Conf.Str(s)        => s.asJson
    case Conf.Num(num)      => num.asJson
    case Conf.Bool(bool)    => bool.asJson
    case Conf.Lst(values)   => values.asJson
    case conf @ Conf.Obj(_) => conf.map.asJson
  }

  implicit val confObjConfDecoder: ConfDecoder[Conf.Obj] = ConfDecoder.instance {
    case conf @ Conf.Obj(_) => Configured.ok(conf)
  }

  implicit val confConfDecoder: ConfDecoder[Conf] = ConfDecoder.instanceF(Configured.ok)

  implicit val uuidConfDecoder: ConfDecoder[UUID] = ConfDecoder.instance {
    case Conf.Str(s) => Try(UUID.fromString(s)).fold(Configured.exception(_), Configured.ok)
  }

  implicit val urlConfDecoder: ConfDecoder[URL] = ConfDecoder.instance {
    case Conf.Str(s) => Try(new URL(s)).fold(Configured.exception(_), Configured.ok)
  }
  implicit val urlDecoder: Decoder[URL] = Decoder.decodeString.emapTry(s => Try(new URL(s)))
  implicit val urlEncoder: Encoder[URL] = Encoder.encodeString.contramap(_.toString)

  implicit val uriConfDecoder: ConfDecoder[URI] = ConfDecoder.instance {
    case Conf.Str(s) => Try(new URI(s)).fold(Configured.exception(_), Configured.ok)
  }
  implicit val uriDecoder: Decoder[URI] = Decoder.decodeString.emapTry(s => Try(new URI(s)))
  implicit val uriEncoder: Encoder[URI] = Encoder.encodeString.contramap(_.toString)

  implicit def catalogTypeConfDecoder[A <: CatalogType](implicit classTag: ClassTag[A]): ConfDecoder[A] =
    ConfDecoder.instance {
      case Conf.Str(id) =>
        Sponge.getRegistry
          .getType(classTag.runtimeClass.asInstanceOf[Class[A]], id)
          .toOption
          .fold[Configured[A]](Configured.error(s"$id is not a valid $classTag"))(Configured.ok)
    }
  implicit def catalogTypeDecoder[A <: CatalogType](implicit classTag: ClassTag[A]): Decoder[A] =
    Decoder.decodeString.emap { id =>
      Sponge.getRegistry
        .getType(classTag.runtimeClass.asInstanceOf[Class[A]], id)
        .toOption
        .toRight(s"$id is not a valid $classTag")
    }
  implicit def catalogTypeEncoder[A <: CatalogType]: Encoder[A] = Encoder.encodeString.contramap(_.getId)

  implicit def dataSerializableConfDecoder[A <: DataSerializable: Decoder: ClassTag]: ConfDecoder[A] =
    ConfDecoder.instanceF { conf =>
      conf.asJson.as[A].fold(Configured.exception(_), Configured.ok)
    }

  implicit def dataSerializableDecoder[A <: DataSerializable](implicit classTag: ClassTag[A]): Decoder[A] =
    Decoder.instance { c =>
      Sponge.getDataManager
        .deserialize(classTag.runtimeClass.asInstanceOf[Class[A]], KatLibDataFormats.readJson(c.value.noSpaces))
        .toOption
        .toRight(DecodingFailure(s"Invalid $classTag", c.history));
    }

  implicit def dataSerializableEncoder[A <: DataSerializable]: Encoder[A] =
    Encoder.instance { a =>
      parser.parse(KatLibDataFormats.writeJson(a.toContainer)).right.get
    }

  implicit val textFormatConfDecoder: ConfDecoder[TextFormat] = ConfDecoder.instanceF { conf =>
    val confColor = conf.getOrElse("color")(TextColors.NONE)
    val confStyle =
      conf.get[Option[Seq[TextStyle.Base]]]("style").map(_.fold(TextStyles.NONE)(styles => TextStyles.of(styles: _*)))
    confColor.product(confStyle).map {
      case (color, style) =>
        TextFormat.NONE.color(color).style(style)
    }
  }
  implicit val textFormatDecoder: Decoder[TextFormat] = Decoder.instance { hcursor =>
    for {
      color <- hcursor.get[Option[TextColor]]("color")
      style <- hcursor.get[Option[Seq[TextStyle.Base]]]("style")
    } yield
      TextFormat.NONE
        .color(color.getOrElse(TextColors.NONE))
        .style(style.fold(TextStyles.NONE)(styles => TextStyles.of(styles: _*)))
  }
  implicit val textFormatEncoder: Encoder[TextFormat] = Encoder.instance { format =>
    Json.obj(
      "color" -> format.getColor.asJson,
      "style" -> Sponge.getRegistry
        .getAllOf(classOf[TextStyle.Base])
        .asScala
        .filter(format.getStyle.contains(_))
        .asJson
    )
  }

  implicit val textTemplateConfDecoder: ConfDecoder[TextTemplate] = ConfDecoder.instance {
    case obj @ Conf.Obj(_) =>
      val openArgConf   = obj.getOrElse("openArg")(TextTemplate.DEFAULT_OPEN_ARG)
      val closeArgConf  = obj.getOrElse("closeArg")(TextTemplate.DEFAULT_CLOSE_ARG)
      val argumentsConf = obj.get[Conf.Obj]("arguments")
      val contentConf   = obj.get[Text]("content")

      openArgConf
        .product(closeArgConf)
        .product(argumentsConf)
        .product(contentConf)
        .andThen {
          case (((openArg, closeArg), arguments), content) =>
            def parse(content: Text, into: Seq[AnyRef]): Configured[Seq[AnyRef]] = {
              val withContent = if (isArg(content)) {
                parseArg(content.asInstanceOf[LiteralText], into)
              } else {
                Configured.ok(into :+ content.toBuilder.removeAll().build)
              }
              content.getChildren.asScala
                .foldLeft(withContent)((accConf, child) => accConf.andThen(acc => parse(child, acc)))
            }

            def parseArg(source: LiteralText, into: Seq[AnyRef]): Configured[Seq[AnyRef]] = {
              val name     = unwrap(source.getContent)
              val nameConf = arguments.get[Conf](name)

              val optionalConf     = nameConf.andThen(_.get[Boolean]("optional"))
              val defaultValueConf = nameConf.andThen(_.get[Text]("defaultValue"))
              val format           = source.getFormat

              optionalConf.product(defaultValueConf).map {
                case (optional, defaultValue) =>
                  into :+ TextTemplate.arg(name).format(format).optional(optional).defaultValue(defaultValue).build
              }
            }

            def isArg(element: Text): Boolean = {
              if (!element.isInstanceOf[LiteralText]) {
                false
              } else {
                val literal: String = element.asInstanceOf[LiteralText].getContent
                literal.startsWith(openArg) && literal.endsWith(closeArg) && isArgDefined(unwrap(literal))
              }
            }

            def unwrap(str: String): String =
              str.substring(openArg.length, str.length - closeArg.length)

            def isArgDefined(argName: String): Boolean = arguments.map.contains(argName)

            parse(content, Nil)
        }
        .map { allArgs =>
          TextTemplate.of(allArgs: _*)
        }
  }
  implicit val textTemplateDecoder: Decoder[TextTemplate] = Decoder.instance { hcursor =>
    val argsCursor = hcursor.downField("arguments")
    for {
      openArg  <- hcursor.getOrElse("openArg")(TextTemplate.DEFAULT_OPEN_ARG)
      closeArg <- hcursor.getOrElse("closeArg")(TextTemplate.DEFAULT_OPEN_ARG)
      content  <- hcursor.get[Text]("content")
      args <- {
        def parse(content: Text, into: Seq[AnyRef]): Either[DecodingFailure, Seq[AnyRef]] = {
          val withContent =
            if (isArg(content)) parseArg(content.asInstanceOf[LiteralText], into)
            else Right(into :+ content.toBuilder.removeAll().build)

          content.getChildren.asScala.foldLeft(withContent)((eAcc, child) => eAcc.flatMap(acc => parse(child, acc)))
        }

        def parseArg(source: LiteralText, into: Seq[AnyRef]): Either[DecodingFailure, Seq[AnyRef]] = {
          val name   = unwrap(source.getContent)
          val format = source.getFormat
          for {
            optional     <- argsCursor.downField(name).getOrElse("optional")(false)
            defaultValue <- argsCursor.downField(name).get[Text]("defaultValue")
          } yield into :+ TextTemplate.arg(name).format(format).optional(optional).defaultValue(defaultValue).build
        }

        def isArg(element: Text) = {
          if (!element.isInstanceOf[LiteralText]) false
          else {
            val literal = element.asInstanceOf[LiteralText].getContent
            literal.startsWith(openArg) && literal.endsWith(closeArg) && isArgDefined(unwrap(literal))
          }
        }

        def unwrap(str: String) =
          str.substring(openArg.length, str.length - closeArg.length)

        def isArgDefined(argName: String) =
          argsCursor.downField(argName).succeeded

        parse(content, Nil)
      }
    } yield TextTemplate.of(args: _*)
  }
  implicit val textTemplateEncoder: Encoder[TextTemplate] = Encoder.instance { template =>
    Json.obj(
      "options" -> Json
        .obj("openArg" -> template.getOpenArgString.asJson, "closeArg" -> template.getCloseArgString.asJson),
      "arguments" -> template.getArguments.asScala.mapValues { arg =>
        Json.obj("optional" -> arg.isOptional.asJson, "defaultValue" -> arg.getDefaultValue.toOption.asJson)
      }.asJson,
      "content" -> template.toText.asJson
    )
  }
}
object SpongeProtocol extends SpongeProtocol
