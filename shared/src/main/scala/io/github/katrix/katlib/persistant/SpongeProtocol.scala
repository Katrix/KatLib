package io.github.katrix.katlib.persistant

import scala.util.Try

import org.spongepowered.api.text.{BookView, Text, TextTemplate}

import com.google.common.reflect.TypeToken
import com.typesafe.config.ConfigValue

import io.circe._
import ninja.leaping.configurate.{ConfigurationNode, SimpleConfigurationNode}
import ninja.leaping.configurate.commented.{CommentedConfigurationNode, SimpleCommentedConfigurationNode}
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import io.github.katrix.katlib.helper.Implicits.typeToken

object SpongeProtocol {

  private val readConfigValue =
    classOf[HoconConfigurationLoader].getMethod("readConfigValue", classOf[ConfigValue], classOf[CommentedConfigurationNode])
  private val fromValue = classOf[HoconConfigurationLoader].getMethod("fromValue", classOf[ConfigurationNode])
  readConfigValue.setAccessible(true)
  fromValue.setAccessible(true)

  private val dummyLoader = HoconConfigurationLoader.builder().build()

  implicit val textEncoder:         Encoder[Text]         = cfgNodeEncoder[Text]
  implicit val textDecoder:         Decoder[Text]         = cfgNodeDecoder[Text]
  implicit val textTemplateEncoder: Encoder[TextTemplate] = cfgNodeEncoder[TextTemplate]
  implicit val textTemplateDecoder: Decoder[TextTemplate] = cfgNodeDecoder[TextTemplate]
  implicit val bookViewEncoder:     Encoder[BookView]     = cfgNodeEncoder[BookView]
  implicit val bookViewDecoder:     Decoder[BookView]     = cfgNodeDecoder[BookView]

  private def cfgNodeEncoder[A: TypeToken]: Encoder[A] = (a: A) => {
    val node      = SimpleConfigurationNode.root()
    val typeToken = implicitly[TypeToken[A]]
    node.setValue(typeToken, a)
    val cfgValue = fromValue.invoke(dummyLoader, node).asInstanceOf[ConfigValue]

    HoconFileStorage.hoconToJson(cfgValue)
  }

  private def cfgNodeDecoder[A: TypeToken]: Decoder[A] = (c: HCursor) => {
    val cfgValue = HoconFileStorage.jsonToHocon(c.value, NoComment)
    val node     = SimpleCommentedConfigurationNode.root()
    readConfigValue.invoke(dummyLoader, cfgValue, node)
    val typeToken = implicitly[TypeToken[A]]

    Try(Option(node.getValue[A](typeToken)).getOrElse(throw DecodingFailure("Missing value", c.history))).toEither.left
      .map(e => DecodingFailure.fromThrowable(e, c.history))
  }

}
