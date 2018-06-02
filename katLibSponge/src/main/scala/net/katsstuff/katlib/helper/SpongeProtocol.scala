package net.katsstuff.katlib.helper

import java.net.{URI, URL}

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
import net.katsstuff.katlib.KatLibDataFormats

trait SpongeProtocol {

  implicit val urlDecoder: Decoder[URL] = Decoder.decodeString.emapTry(s => Try(new URL(s)))
  implicit val urlEncoder: Encoder[URL] = Encoder.encodeString.contramap(_.toString)

  implicit val uriDecoder: Decoder[URI] = Decoder.decodeString.emapTry(s => Try(new URI(s)))
  implicit val uriEncoder: Encoder[URI] = Encoder.encodeString.contramap(_.toString)

  implicit def catalogTypeDecoder[A <: CatalogType](implicit classTag: ClassTag[A]): Decoder[A] =
    Decoder.decodeString.emap { id =>
      Sponge.getRegistry
        .getType(classTag.runtimeClass.asInstanceOf[Class[A]], id)
        .toOption
        .toRight(s"$id is not a valid $classTag")
    }
  implicit def catalogTypeEncoder[A <: CatalogType]: Encoder[A] = Encoder.encodeString.contramap(_.getId)

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
