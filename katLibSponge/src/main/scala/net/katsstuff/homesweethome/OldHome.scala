package net.katsstuff.homesweethome

import net.katsstuff.katlib.helpers.KatCirceDecoder
import org.spongepowered.api.ResourceKey

import scala.jdk.CollectionConverters._

import java.nio.file.{Files, Path}
import java.util.UUID

case class OldHome(
  x: Double,
  y: Double,
  z: Double,
  yaw: Double,
  pitch: Double,
  world: UUID,
  residents: Option[Vector[UUID]]
) derives KatCirceDecoder

object OldHome:
  def loadOldHomes(path: Path): Option[Map[UUID, Map[String, OldHome]]] = 
    Option.when(Files.exists(path)) {
      val fileContent = Files.readAllLines(path).asScala.mkString("\n")
      
      for
        json <- io.circe.parser.parse(fileContent)
        cursor = json.hcursor
        home <- cursor.get[Map[UUID, Map[String, OldHome]]]("home")
      yield
        Files.move(path, path.resolveSibling(path.getFileName.toString + ".old"))
        home
    }.flatMap(_.toOption)