package io.github.katrix.katlib.persistant

import java.lang.{Boolean => JBoolean}
import java.nio.file.Path
import java.util.{List => JList, Map => JMap}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.typesafe.config.{ConfigFactory, ConfigList, ConfigObject, ConfigRenderOptions, ConfigValue, ConfigValueFactory}

import io.circe._
import io.circe.syntax._

class HoconFileStorage[A: Decoder: Encoder](
    path:  Path
)(options: ConfigRenderOptions = ConfigRenderOptions.defaults().setComments(true).setFormatted(true))
    extends FileStorageBase[A](path) {

  override def loadData(): Future[A] =
    Future {
      val root = ConfigFactory.parseFile(path.toFile).root()
      HoconFileStorage.hoconToJson(root).as[A].fold(Future.failed, Future.successful)
    }.flatten

  override def saveData(data: A): Future[Unit] = Future {
    HoconFileStorage.jsonToHocon(data.asJson).render(options)
  }
}
object HoconFileStorage {

  def hoconToJson(configValue: ConfigValue): Json = {
    def valueToJson: Json = {
      configValue.unwrapped() match {
        case str:  String => Json.fromString(str)
        case num:  Number => Json.fromDouble(num.doubleValue()).getOrElse(Json.fromLong(num.longValue()))
        case bool: JBoolean => Json.fromBoolean(bool)
        case _:    JMap[String @unchecked, AnyRef @unchecked] =>
          Json.obj(configValue.asInstanceOf[ConfigObject].asScala.mapValues(hoconToJson).toSeq: _*)
        case _: JList[AnyRef @unchecked] => Json.arr(configValue.asInstanceOf[ConfigList].asScala.map(hoconToJson): _*)
        case null => Json.Null
      }
    }

    val comments = configValue.origin().comments().asScala
    if (comments.nonEmpty) {
      val comment = comments.mkString("\n")
      Json.obj("comment" -> Json.fromString(comment), "value" -> valueToJson)
    } else valueToJson
  }

  def jsonToHocon(json: Json): ConfigValue = {
    val c = json.hcursor

    val res = for {
      comment <- c.get[String]("comment").toOption
      value   <- c.downField("value").focus
    } yield {
      value match {
        case _ if value.isString  => ConfigValueFactory.fromAnyRef(value.asString.get, comment)
        case _ if value.isNumber  => ConfigValueFactory.fromAnyRef(value.asNumber.get.toDouble, comment)
        case _ if value.isBoolean => ConfigValueFactory.fromAnyRef(value.asBoolean.get, comment)
        case _ if value.isObject  => ConfigValueFactory.fromMap(value.asObject.get.toMap.mapValues(jsonToHocon).asJava, comment)
        case _ if value.isArray   => ConfigValueFactory.fromIterable(value.asArray.get.map(jsonToHocon).asJava, comment)
        case _ if value.isNull    => ConfigValueFactory.fromAnyRef(null, comment)
      }
    }

    res.getOrElse {
      json match {
        case _ if json.isString  => ConfigValueFactory.fromAnyRef(json.asString.get)
        case _ if json.isNumber  => ConfigValueFactory.fromAnyRef(json.asNumber.get.toDouble)
        case _ if json.isBoolean => ConfigValueFactory.fromAnyRef(json.asBoolean.get)
        case _ if json.isObject  => ConfigValueFactory.fromMap(json.asObject.get.toMap.mapValues(jsonToHocon).asJava)
        case _ if json.isArray   => ConfigValueFactory.fromIterable(json.asArray.get.map(jsonToHocon).asJava)
        case _ if json.isNull    => ConfigValueFactory.fromAnyRef(null)
      }
    }
  }
}
