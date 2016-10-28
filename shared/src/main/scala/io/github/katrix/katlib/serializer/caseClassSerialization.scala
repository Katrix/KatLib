package io.github.katrix.katlib.serializer

import scala.util.{Success, Try}

import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.helper.LogHelper
import io.github.katrix.katlib.serializer.CaseSerializers.{ConfigNode, TypeClassSerializer}
import shapeless._
import shapeless.labelled.{FieldType, field}

object CaseSerializers {

	//We hide the implementation behind these traits so that it's easier to use for other stuff, in addition to implicits
	trait ConfigNode {
		def getParent: ConfigNode
		def getNode(string: String*): ConfigNode
		def read[A: TypeClassSerializer]: Try[A] = implicitly[TypeClassSerializer[A]].read(this)
		def write[A: TypeClassSerializer](value: A): ConfigNode = implicitly[TypeClassSerializer[A]].write(value, this)

		def readBoolean: Try[Boolean]
		def readByte: Try[Byte]
		def readShort: Try[Short]
		def readInt: Try[Int]
		def readLong: Try[Long]
		def readFloat: Try[Float]
		def readDouble: Try[Double]
		def readString: Try[String]

		def writeBoolean(value: Boolean): ConfigNode
		def writeByte(value: Byte): ConfigNode
		def writeShort(value: Short): ConfigNode
		def writeInt(value: Int): ConfigNode
		def writeLong(value: Long): ConfigNode
		def writeFloat(value: Float): ConfigNode
		def writeDouble(value: Double): ConfigNode
		def writeString(value: String): ConfigNode
	}

	trait TypeClassSerializer[A] {
		def write(obj: A, value: ConfigNode): ConfigNode = value.write(obj)(this)
		def read(value: ConfigNode): Try[A] = value.read[A](this)
	}

	implicit object HNilSerializer extends TypeClassSerializer[HNil] {
		override def write(obj: HNil, value: ConfigNode): ConfigNode = value
		override def read(value: ConfigNode): Try[HNil] = Success(HNil)
	}

	implicit def hListSerializer[Key <: Symbol, Value, Remaining <: HList](
			implicit key: Witness.Aux[Key],
			sh: Lazy[TypeClassSerializer[Value]],
			st: Lazy[TypeClassSerializer[Remaining]]): TypeClassSerializer[FieldType[Key, Value] :: Remaining]
	= new TypeClassSerializer[FieldType[Key, Value] :: Remaining] {

		override def write(hList: FieldType[Key, Value] :: Remaining, value: ConfigNode): ConfigNode = {
			val tailValue = st.value.write(hList.tail, value)
			sh.value.write(hList.head, tailValue.getNode(key.value.name)).getParent
		}

		override def read(value: ConfigNode): Try[FieldType[Key, Value] :: Remaining] = {
			val head = sh.value.read(value.getNode(key.value.name))
			val tail = st.value.read(value)

			head.flatMap(h => tail.map(t => field[Key](h) :: t))
		}
	}

	implicit object CNilSerializer extends TypeClassSerializer[CNil] {
		override def write(obj: CNil, value: ConfigNode): ConfigNode = throw new IllegalStateException
		override def read(value: ConfigNode): Try[CNil] = throw new IllegalStateException
	}

	implicit def coProductSerializer[Name <: Symbol, Head, Remaining <: Coproduct](
			implicit key: Witness.Aux[Name],
			sh: Lazy[TypeClassSerializer[Head]],
			st: Lazy[TypeClassSerializer[Remaining]]): TypeClassSerializer[FieldType[Name, Head] :+: Remaining]
	= new TypeClassSerializer[FieldType[Name, Head] :+: Remaining] {

		override def write(obj: FieldType[Name, Head] :+: Remaining, value: ConfigNode): ConfigNode = obj match {
			case Inl(found) =>
				val headValue = sh.value.write(found, value)
				headValue.getNode("type").writeString(key.value.name).getParent
			case Inr(tail) => st.value.write(tail, value)
		}

		override def read(value: ConfigNode): Try[FieldType[Name, Head] :+: Remaining] = {
			if(value.getNode("type").readString.map(_ == key.value.name).getOrElse(false))
				sh.value.read(value).map(h => Inl(field[Name](h)))
			else
				st.value.read(value).map(t => Inr(t))
		}
	}



	implicit def caseSerializer[A, Repr](
			implicit gen: LabelledGeneric.Aux[A, Repr],
			ser: Lazy[TypeClassSerializer[Repr]],
			tpe: Typeable[A],
			plugin: KatPlugin
	): TypeClassSerializer[A] = new TypeClassSerializer[A] {
		LogHelper.trace(s"Creating serializer for ${tpe.describe}")

		override def write(obj: A, value: ConfigNode): ConfigNode = ser.value.write(gen.to(obj), value)
		override def read(value: ConfigNode): Try[A] = ser.value.read(value).map(gen.from)
	}
}

trait DefaultSerializers {

	implicit object BooleanSerializer extends TypeClassSerializer[Boolean] {
		override def write(obj: Boolean, value: ConfigNode): ConfigNode = value.writeBoolean(obj)
		override def read(value: ConfigNode): Try[Boolean] = value.readBoolean
	}

	implicit object ByteSerializer extends TypeClassSerializer[Byte] {
		override def write(obj: Byte, value: ConfigNode): ConfigNode = value.writeByte(obj)
		override def read(value: ConfigNode): Try[Byte] = value.readByte
	}

	implicit object ShortSerializer extends TypeClassSerializer[Short] {
		override def write(obj: Short, value: ConfigNode): ConfigNode = value.writeShort(obj)
		override def read(value: ConfigNode): Try[Short] = value.readShort
	}

	implicit object IntSerializer extends TypeClassSerializer[Int] {
		override def write(obj: Int, value: ConfigNode): ConfigNode = value.writeInt(obj)
		override def read(value: ConfigNode): Try[Int] = value.readInt
	}

	implicit object LongSerializer extends TypeClassSerializer[Long] {
		override def write(obj: Long, value: ConfigNode): ConfigNode = value.writeLong(obj)
		override def read(value: ConfigNode): Try[Long] = value.readLong
	}

	implicit object FloatSerializer extends TypeClassSerializer[Float] {
		override def write(obj: Float, value: ConfigNode): ConfigNode = value.writeFloat(obj)
		override def read(value: ConfigNode): Try[Float] = value.readFloat
	}

	implicit object DoubleSerializer extends TypeClassSerializer[Double] {
		override def write(obj: Double, value: ConfigNode): ConfigNode = value.writeDouble(obj)
		override def read(value: ConfigNode): Try[Double] = value.readDouble
	}

	implicit object StringSerializer extends TypeClassSerializer[String] {
		override def write(obj: String, value: ConfigNode): ConfigNode = value.writeString(obj)
		override def read(value: ConfigNode): Try[String] = value.readString
	}
}

