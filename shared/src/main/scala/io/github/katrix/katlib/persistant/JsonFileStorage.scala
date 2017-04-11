package io.github.katrix.katlib.persistant

import java.nio.file.{Files, Path}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import io.circe._
import io.circe.parser._
import io.circe.syntax._

class JsonFileStorage[A: Decoder: Encoder](path: Path) extends FileStorageBase[A](path) {
  override def loadData(): Future[A] =
    Future {
      createFile()
      parse(Files.readAllLines(file).asScala.mkString("\n")).flatMap(_.as[A]).fold(Future.failed, Future.successful)
    }.flatten

  override def saveData(data: A): Future[Unit] = Future {
    createFile()
    Files.write(file, Seq(data.asJson.noSpaces).asJava)
  }
}
