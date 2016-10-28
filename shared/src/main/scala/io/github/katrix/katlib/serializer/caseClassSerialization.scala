package io.github.katrix.katlib.serializer

import scala.util.{Success, Try}

import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.helper.LogHelper
import io.github.katrix.katlib.serializer.CaseSerializers.{ConfigNode, ConfigSerializer}
import shapeless._
import shapeless.labelled.{FieldType, field}

object CaseSerializers {

	//We hide the implementation behind these traits so that it's easier to use for other stuff, in addition to implicits
	trait ConfigNode {
		def getParent: ConfigNode
		def getNode(string: String*): ConfigNode
		def read[A: ConfigSerializer]: Try[A] = implicitly[ConfigSerializer[A]].read(this)
		def write[A: ConfigSerializer](value: A): ConfigNode = implicitly[ConfigSerializer[A]].write(value, this)

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

	trait ConfigSerializer[A] {
		def write(obj: A, value: ConfigNode): ConfigNode
		def read(value: ConfigNode): Try[A]
	}

	implicit object HNilSerializer extends ConfigSerializer[HNil] {
		override def write(obj: HNil, value: ConfigNode): ConfigNode = value
		override def read(value: ConfigNode): Try[HNil] = Success(HNil)
	}

	implicit def hListSerializer[Key <: Symbol, Value, Remaining <: HList](
			implicit key: Witness.Aux[Key],
			sh: Lazy[ConfigSerializer[Value]],
			st: Lazy[ConfigSerializer[Remaining]]): ConfigSerializer[FieldType[Key, Value] :: Remaining]
	= new ConfigSerializer[FieldType[Key, Value] :: Remaining] {

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

	implicit object CNilSerializer extends ConfigSerializer[CNil] {
		override def write(obj: CNil, value: ConfigNode): ConfigNode = throw new IllegalStateException
		override def read(value: ConfigNode): Try[CNil] = throw new IllegalStateException
	}

	implicit def coProductSerializer[Name <: Symbol, Head, Remaining <: Coproduct](
			implicit key: Witness.Aux[Name],
			sh: Lazy[ConfigSerializer[Head]],
			st: Lazy[ConfigSerializer[Remaining]]): ConfigSerializer[FieldType[Name, Head] :+: Remaining]
	= new ConfigSerializer[FieldType[Name, Head] :+: Remaining] {

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
			ser: Lazy[ConfigSerializer[Repr]],
			tpe: Typeable[A],
			plugin: KatPlugin
	): ConfigSerializer[A] = new ConfigSerializer[A] {
		LogHelper.trace(s"Creating serializer for ${tpe.describe}")

		override def write(obj: A, value: ConfigNode): ConfigNode = ser.value.write(gen.to(obj), value)
		override def read(value: ConfigNode): Try[A] = ser.value.read(value).map(gen.from)
	}
}

trait DefaultSerializers {

	implicit object BooleanSerializer extends ConfigSerializer[Boolean] {
		override def write(obj: Boolean, value: ConfigNode): ConfigNode = value.writeBoolean(obj)
		override def read(value: ConfigNode): Try[Boolean] = value.readBoolean
	}

	implicit object ByteSerializer extends ConfigSerializer[Byte] {
		override def write(obj: Byte, value: ConfigNode): ConfigNode = value.writeByte(obj)
		override def read(value: ConfigNode): Try[Byte] = value.readByte
	}

	implicit object ShortSerializer extends ConfigSerializer[Short] {
		override def write(obj: Short, value: ConfigNode): ConfigNode = value.writeShort(obj)
		override def read(value: ConfigNode): Try[Short] = value.readShort
	}

	implicit object IntSerializer extends ConfigSerializer[Int] {
		override def write(obj: Int, value: ConfigNode): ConfigNode = value.writeInt(obj)
		override def read(value: ConfigNode): Try[Int] = value.readInt
	}

	implicit object LongSerializer extends ConfigSerializer[Long] {
		override def write(obj: Long, value: ConfigNode): ConfigNode = value.writeLong(obj)
		override def read(value: ConfigNode): Try[Long] = value.readLong
	}

	implicit object FloatSerializer extends ConfigSerializer[Float] {
		override def write(obj: Float, value: ConfigNode): ConfigNode = value.writeFloat(obj)
		override def read(value: ConfigNode): Try[Float] = value.readFloat
	}

	implicit object DoubleSerializer extends ConfigSerializer[Double] {
		override def write(obj: Double, value: ConfigNode): ConfigNode = value.writeDouble(obj)
		override def read(value: ConfigNode): Try[Double] = value.readDouble
	}

	implicit object StringSerializer extends ConfigSerializer[String] {
		override def write(obj: String, value: ConfigNode): ConfigNode = value.writeString(obj)
		override def read(value: ConfigNode): Try[String] = value.readString
	}

	

	implicit def SeqSerializer[A: ConfigSerializer] = new ConfigSerializer[Seq[A]] {
		override def write(obj: Seq[A], value: ConfigNode): ConfigNode = ???
		override def read(value: ConfigNode): Try[Seq[A]] = ???
	}

	implicit def SetSerializer[A: ConfigSerializer] = new ConfigSerializer[Set[A]] {
		override def write(obj: Set[A], value: ConfigNode): ConfigNode = ???
		override def read(value: ConfigNode): Try[Set[A]] = ???
	}

	implicit def MapSerializer[A: ConfigSerializer, B: ConfigSerializer] = new ConfigSerializer[Map[A, B]] {
		override def write(obj: Map[A, B], value: ConfigNode): ConfigNode = ???
		override def read(value: ConfigNode): Try[Map[A, B]] = ???
	}
}

