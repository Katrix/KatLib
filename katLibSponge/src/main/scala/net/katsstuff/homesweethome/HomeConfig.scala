package net.katsstuff.homesweethome

import io.circe.Decoder
import net.katsstuff.katlib.helpers.KatCirceDecoder

import java.nio.file.{Files, Path}
import scala.concurrent.duration.FiniteDuration
import scala.util.Try
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

case class HomeConfig(homeLimit: Int, residentLimit: Int, timeout: Int) derives KatCirceDecoder:
  def timeoutDuration: FiniteDuration = timeout.seconds

object HomeConfig:
  
  def load(path: Path): Try[HomeConfig] =
    HomeSweetHome.logger.info("Loading config")
    Try {
      Files.createDirectories(path.getParent)
      if Files.notExists(path) then
        HomeSweetHome.asset("config.conf").copyToFile(path, false, true)

      Files.readAllLines(path).asScala.mkString("\n")
    }.flatMap(s => io.circe.parser.parse(s).toTry).flatMap { json =>
      val cursor = json.hcursor
      cursor.get[Int]("version").flatMap {
        case 2 => cursor.get[HomeConfig]("home")
        case _ => Left(Exception("Unsupported version"))
      }.toTry
    }