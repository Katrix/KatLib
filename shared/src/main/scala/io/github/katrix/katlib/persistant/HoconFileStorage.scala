package io.github.katrix.katlib.persistant

import java.lang.{Boolean => JBoolean}
import java.nio.file.{Files, Path}
import java.util
import java.util.{List => JList, Map => JMap}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.typesafe.config.{ConfigFactory, ConfigList, ConfigObject, ConfigRenderOptions, ConfigValue, ConfigValueFactory}

import io.circe._
import io.circe.syntax._

class HoconFileStorage[A: Decoder: Encoder](path: Path, comments: CommentTree = NoComment)(
    options: ConfigRenderOptions = ConfigRenderOptions.defaults().setComments(true).setFormatted(true)
) extends FileStorageBase[A](path) {

  override def loadData(): Future[A] =
    Future {
      val root = ConfigFactory.parseFile(path.toFile).root()
      HoconFileStorage.hoconToJson(root).as[A].fold(Future.failed, Future.successful)
    }.flatten

  override def saveData(data: A): Future[Unit] = Future {
    Files.write(file, util.Arrays.asList(HoconFileStorage.jsonToHocon(data.asJson, comments).render(options).split('\n'): _*))
  }
}
object HoconFileStorage {

  def hoconToJson(configValue: ConfigValue): Json = {
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

  def jsonToHocon(json: Json, comments: CommentTree): ConfigValue = {
    val cfgValue = json.fold(
      ConfigValueFactory.fromAnyRef(null),
      bool => ConfigValueFactory.fromAnyRef(bool),
      num => ConfigValueFactory.fromAnyRef(num.toDouble),
      str => ConfigValueFactory.fromAnyRef(str),
      arr => {
        val values = arr.zipWithIndex.map { case (child, i) => jsonToHocon(child, comments.getChild(i.toString)) }
        ConfigValueFactory.fromIterable(values.asJava)
      },
      obj => {
        val values = obj.toMap.map { case (str, child) => str -> jsonToHocon(child, comments.getChild(str)) }
        ConfigValueFactory.fromMap(values.asJava)
      }
    )

    comments match {
      case CommentNode(comment) => cfgValue.withOrigin(cfgValue.origin().withComments(util.Arrays.asList(comment.split('\n'): _*)))
      case _ => cfgValue
    }
  }
}
