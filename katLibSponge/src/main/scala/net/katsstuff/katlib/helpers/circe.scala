package net.katsstuff.katlib.helpers

import scala.language.implicitConversions

import io.circe._
import perspective._
import perspective.derivation._

import scala.compiletime.{erasedValue, summonFrom, summonInline}

trait KatCirceDecoder[A] extends Decoder[A]
object KatCirceDecoder:
  def derivedProductDecoder[A](
    using gen: HKDProductGeneric[A],
    decoders: gen.Gen[Decoder]
  ): KatCirceDecoder[A] = new KatCirceDecoder[A]:
    override def apply(cursor: HCursor): Decoder.Result[A] =
      import gen.given
    
      gen.names
        .map2K(decoders)(
          [Z] => (name: String, decoder: Decoder[Z]) => cursor.get(name)(using decoder)
        )
        .sequenceIdK
        .map(gen.from)

  def derivedSumDecoder[A](
      using gen: HKDSumGeneric[A],
      decoders: gen.Gen[Decoder]
  ): KatCirceDecoder[A] = new KatCirceDecoder[A]:
    override def apply(cursor: HCursor): Decoder.Result[A] =
      import gen.given

      for
        typeName <- cursor.get[String]("$type")
        index <- gen.nameToIndexMap.get(typeName).toRight(DecodingFailure(s"$typeName is not a valid ${gen.typeName}", cursor.downField("$type").history))
        decoder: Decoder[_ <: A] = decoders.indexK(index) //TODO Type needed. Inffered to Any otherwise
        res <- decoder(cursor.get[Json]("$value").fold(_ => cursor, _.hcursor))
      yield res

  private inline def caseDecoders[XS <: Tuple]: Tuple.Map[XS, Decoder] = inline erasedValue[XS] match
    case _: EmptyTuple => EmptyTuple
    case _: (h *: t) =>
      val headDecoder: Decoder[h] = summonFrom {
        case d: Decoder[`h`] => d
        case gen: HKDGeneric[`h`] => derived[h]
      }
      headDecoder *: caseDecoders[t]

  inline given derived[A](using gen: HKDGeneric[A]): KatCirceDecoder[A] = inline gen match
    case gen: HKDProductGeneric.Aux[A, gen.Gen] =>
      given gen.Gen[Decoder] = summonInline[gen.Gen[Decoder]]
      derivedProductDecoder(using gen)
    case gen: HKDSumGeneric.Aux[A, gen.Gen] =>
      summonFrom {
        case decoders: gen.Gen[Decoder] => derivedSumDecoder(using gen, decoders)
        case _ =>
          val decodersTuple = caseDecoders[gen.TupleRep]
          val decoders = gen.tupleToGen(decodersTuple)
          derivedSumDecoder(using gen, decoders)
      }
end KatCirceDecoder

trait KatCirceEncoder[A] extends Encoder[A]
object KatCirceEncoder:
  def derivedProductEncoder[A](
      using gen: HKDProductGeneric[A],
      encoders: gen.Gen[Encoder]
  ): KatCirceEncoder[A] = new KatCirceEncoder[A]:
    override def apply(a: A): Json =
      import gen.given

      val list: List[(String, Json)] =
        gen
          .to(a)
          .map2Const(encoders)([Z] => (caseObj: Z, encoder: Encoder[Z]) => encoder(caseObj))
          //TODO Type needed
          .map2Const[Const[Json], Const[String], (String, Json), Nothing](gen.names)([Z] => (json: Json, name: String) => (name, json))
          .toListK
    
      Json.obj(list: _*)

  def derivedSumEncoder[A](
      using gen: HKDSumGeneric[A],
      encoders: gen.Gen[Encoder]
  ): KatCirceEncoder[A] = new KatCirceEncoder[A]:
    override def apply(a: A): Json =
      import gen.given

      val typeName = Json.fromString(gen.indexToNameMap(gen.indexOf(a)))

      val encodings = 
        gen
          .to(a)
          .map2Const(encoders)([Z] => (optCase: Option[Z], encoder: Encoder[Z]) => optCase.map(x => encoder(x)))

      val json = encodings.indexK(gen.indexOf(a)).get
      json.asObject match
        case Some(jsObj) => Json.fromJsonObject(jsObj.add("$type", typeName))
        case None        => Json.obj("$type" -> typeName, "$value" -> json)

  private inline def caseEncoders[XS <: Tuple]: Tuple.Map[XS, Encoder] = inline erasedValue[XS] match
    case _: EmptyTuple => EmptyTuple
    case _: (h *: t) => 
      val headEncoder: Encoder[h] = summonFrom {
        case e: Encoder[`h`] => e
        case gen: HKDGeneric[`h`] => derived[h]
      }
      headEncoder *: caseEncoders[t]

  inline given derived[A](using gen: HKDGeneric[A]): Encoder[A] = inline gen match
    case gen: HKDProductGeneric.Aux[A, gen.Gen] => 
      given gen.Gen[Encoder] = summonInline[gen.Gen[Encoder]]
      derivedProductEncoder(using gen)
    case gen: HKDSumGeneric.Aux[A, gen.Gen] =>
      summonFrom {
        case encoders: gen.Gen[Encoder] => derivedSumEncoder(using gen, encoders)
        case _ =>
          val encodersTuple = caseEncoders[gen.TupleRep]
          val encoders = gen.tupleToGen(encodersTuple)
          derivedSumEncoder(using gen, encoders)
      }
end KatCirceEncoder
