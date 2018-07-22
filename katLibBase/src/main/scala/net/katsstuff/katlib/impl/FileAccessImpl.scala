package net.katsstuff.katlib.impl

import java.nio.file.{Files, Path}

import scala.collection.JavaConverters._

import cats.effect.Sync
import net.katsstuff.katlib.algebras.FileAccess

class FileAccessImpl[F[_]](implicit F: Sync[F]) extends FileAccess[F] {
  override def readFile(file: Path): F[String] = F.delay(Files.readAllLines(file).asScala.mkString("\n"))

  override def saveFile(file: Path, content: String): F[Unit] = F.delay(Files.write(file, content.lines.toSeq.asJava))

  override def exist(file: Path): F[Boolean] = F.delay(Files.exists(file))
}
