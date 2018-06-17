package net.katstuff.katlib.algebras

import java.nio.file.Path

/**
  * Provides basic file read write access.
  */
trait FileAccess[F[_]] {

  /**
    * Reads the contents of the file if it exists.
    * @param file Where to read from.
    */
  def readFile(file: Path): F[String]

  /**
    * Writes stuff to a file, and creates it if it does not exist.
    * @param file Where to write to.
    * @param content What to write to the file.
    */
  def saveFile(file: Path, content: String): F[Unit]

  /**
    * Checks if a given file or folder exists.
    * @param file The location to check
    * @return If the location exists
    */
  def exist(file: Path): F[Boolean]
}
