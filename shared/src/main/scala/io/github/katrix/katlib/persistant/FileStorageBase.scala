package io.github.katrix.katlib.persistant

import java.nio.file.{FileAlreadyExistsException, Files, Path}

import scala.concurrent.Future
import scala.util.control.NonFatal

abstract class FileStorageBase[A](val file: Path) {

  createFile()

  protected def createFile(): Unit = {
    try {
      Files.createDirectories(file.getParent)
      Files.createFile(file)
    } catch {
      case _: FileAlreadyExistsException => //Ignore
      case NonFatal(e) => e.printStackTrace()
    }
  }

  def loadData(): Future[A]
  def saveData(data: A): Future[Unit]
}
