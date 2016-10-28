package io.github.katrix.katlib.serializer

import scala.util.{Failure, Success, Try}

import com.google.common.reflect.TypeToken

import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.helper.LogHelper
import io.github.katrix.katlib.serializer.ConfigSerializerBase.{ConfigNode, ConfigSerializer}
import shapeless._
import shapeless.labelled.{FieldType, field}

object ConfigSerializerBase {

	//We hide the implementation behind these traits so that it's easier to use for other stuff, in addition to implicits
	trait ConfigNode {
		def getParent: ConfigNode
		def getNode(string: String*): ConfigNode
		def getChildren: Seq[ConfigNode]

		def read[A: ConfigSerializer]: Try[A]
		def write[A: ConfigSerializer](value: A): ConfigNode

		def readList[A: ConfigSerializer]: Try[Seq[A]]
		def writeList[A: ConfigSerializer](value: Seq[A]): ConfigNode
	}

	trait ConfigSerializer[A] {
		def shouldBypass: Option[TypeToken[A]] = None
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
			st: Lazy[ConfigSerializer[Remaining]],
			stringSerializer: ConfigSerializer[String]): ConfigSerializer[FieldType[Name, Head] :+: Remaining]
	= new ConfigSerializer[FieldType[Name, Head] :+: Remaining] {

		override def write(obj: FieldType[Name, Head] :+: Remaining, value: ConfigNode): ConfigNode = obj match {
			case Inl(found) =>
				val headValue = sh.value.write(found, value)
				headValue.getNode("type").write[String](key.value.name).getParent
			case Inr(tail) => st.value.write(tail, value)
		}

		override def read(value: ConfigNode): Try[FieldType[Name, Head] :+: Remaining] = {
			if(value.getNode("type").read[String].map(_ == key.value.name).getOrElse(false))
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

	val BooleanSerializer: ConfigSerializer[Boolean]
	val ByteSerializer   : ConfigSerializer[Byte]
	val ShortSerializer  : ConfigSerializer[Short]
	val IntSerializer    : ConfigSerializer[Int]
	val LongSerializer   : ConfigSerializer[Long]
	val FloatSerializer  : ConfigSerializer[Float]
	val DoubleSerializer : ConfigSerializer[Double]
	val StringSerializer : ConfigSerializer[String]

	def SeqSerializer[A: ConfigSerializer]: ConfigSerializer[Seq[A]]
	def MapSerializer[A: ConfigSerializer, B: ConfigSerializer]: ConfigSerializer[Map[A, B]]
}

trait DefaultSerializersImpl extends DefaultSerializers {

	import io.github.katrix.katlib.helper.Implicits.typeToken

	private def primitiveSerializer[A](typeToken: TypeToken[A]) = new ConfigSerializer[A] {
		override def shouldBypass: Option[TypeToken[A]] = Some(typeToken)
		override def write(obj: A, value: ConfigNode): ConfigNode = value.write(obj)(this)
		override def read(value: ConfigNode): Try[A] = value.read(this)
	}

	implicit val BooleanSerializer = primitiveSerializer(typeToken[Boolean])
	implicit val ByteSerializer = primitiveSerializer(typeToken[Byte])
	implicit val ShortSerializer = primitiveSerializer(typeToken[Short])
	implicit val IntSerializer = primitiveSerializer(typeToken[Int])
	implicit val LongSerializer = primitiveSerializer(typeToken[Long])
	implicit val FloatSerializer = primitiveSerializer(typeToken[Float])
	implicit val DoubleSerializer = primitiveSerializer(typeToken[Double])
	implicit val StringSerializer = primitiveSerializer(typeToken[String])

	implicit def SeqSerializer[A: ConfigSerializer] = new ConfigSerializer[Seq[A]] {
		override def write(obj: Seq[A], value: ConfigNode): ConfigNode = value.writeList(obj)
		override def read(value: ConfigNode): Try[Seq[A]] = value.readList[A]
	}


	implicit def MapSerializer[A: ConfigSerializer, B: ConfigSerializer] = new ConfigSerializer[Map[A, B]] {
		override def write(obj: Map[A, B], value: ConfigNode): ConfigNode = {
			obj.foldRight(value){case ((k, v), node) => node.write(k).write(v)}
		}
		override def read(value: ConfigNode): Try[Map[A, B]] = {
			def flatten[T](xs: Seq[Try[T]]): Try[Seq[T]] = {
				val (ss: Seq[Success[T]]@unchecked, fs: Seq[Failure[T]]@unchecked) =
					xs.partition(_.isSuccess)

				if (fs.isEmpty) Success(ss map (_.get))
				else Failure[Seq[T]](fs.head.exception) // Only keep the first failure
			}

			val trySeq = value.getChildren.map(node => node.read[A].flatMap(a => node.read[B].map(b => (a, b))))

			flatten(trySeq).map(_.toMap)
		}
	}
}

