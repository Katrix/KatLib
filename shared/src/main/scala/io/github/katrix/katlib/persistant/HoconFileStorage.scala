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
      value.fold(
        ConfigValueFactory.fromAnyRef(null, comment),
        bool => ConfigValueFactory.fromAnyRef(bool, comment),
        num => ConfigValueFactory.fromAnyRef(num.toDouble, comment),
        str => ConfigValueFactory.fromAnyRef(str, comment),
        arr => ConfigValueFactory.fromIterable(arr.map(jsonToHocon).asJava, comment),
        obj => ConfigValueFactory.fromMap(obj.toMap.mapValues(jsonToHocon).asJava, comment)
      )
    }

    res.getOrElse {
      json.fold(
        ConfigValueFactory.fromAnyRef(null),
        bool => ConfigValueFactory.fromAnyRef(bool),
        num => ConfigValueFactory.fromAnyRef(num.toDouble),
        str => ConfigValueFactory.fromAnyRef(str),
        arr => ConfigValueFactory.fromIterable(arr.map(jsonToHocon).asJava),
        obj => ConfigValueFactory.fromMap(obj.toMap.mapValues(jsonToHocon).asJava)
      )
    }
  }
}
