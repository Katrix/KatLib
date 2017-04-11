package io.github.katrix.katlib.persistant

import io.circe._
import io.circe.syntax._
import shapeless._

case class CommentedConfigValueT[A, Comment <: String](value: A)
object CommentedConfigValueT {

  implicit def encoder[A: Encoder, Comment <: String](implicit witness: Witness.Aux[Comment]): Encoder[CommentedConfigValueT[A, Comment]] =
    (a: CommentedConfigValueT[A, Comment]) => {
      Json.obj("value" -> a.value.asJson, "comment" -> Json.fromString(witness.value))
    }

  implicit def decoder[A: Decoder, Comment <: String](implicit witness: Witness.Aux[Comment]): Decoder[CommentedConfigValueT[A, Comment]] =
    (c: HCursor) => {
      for {
        value   <- c.get[A]("value")
        comment <- c.get[String]("comment")
        _       <- Either.cond(comment == witness.value, (), DecodingFailure("Comment did not match expected content", c.history))
      } yield CommentedConfigValueT(value)
    }
}
