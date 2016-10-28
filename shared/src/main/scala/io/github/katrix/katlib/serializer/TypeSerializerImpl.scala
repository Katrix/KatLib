package io.github.katrix.katlib.serializer

import scala.collection.JavaConverters._
import scala.util.Try

import com.google.common.reflect.TypeToken

import io.github.katrix.katlib.helper.Implicits.typeToken
import io.github.katrix.katlib.serializer.CaseSerializers.{ConfigNode, ConfigSerializer}
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer
import shapeless.Strict

object TypeSerializerImpl extends DefaultSerializers {

	implicit class ConfigurationNodeWrapper(val node: ConfigurationNode) extends ConfigNode {

		override def getParent: ConfigNode = new ConfigurationNodeWrapper(node.getParent)
		override def getNode(string: String*): ConfigNode = new ConfigurationNodeWrapper(node.getNode(string: _*))

		def readValue[A: TypeToken]: Try[A] = Try(node.getValue(implicitly[TypeToken[A]]))

		override def readBoolean: Try[Boolean] = readValue(typeToken[Boolean])
		override def readByte: Try[Byte] = readValue(typeToken[Byte])
		override def readShort: Try[Short] = readValue(typeToken[Short])
		override def readInt: Try[Int] = readValue(typeToken[Int])
		override def readLong: Try[Long] = readValue(typeToken[Long])
		override def readFloat: Try[Float] = readValue(typeToken[Float])
		override def readDouble: Try[Double] = readValue(typeToken[Double])
		override def readString: Try[String] = readValue(typeToken[String])

		def writeValue(value: AnyRef): ConfigNode = {node.setValue(value); this}
		def writeValue[A: TypeToken](value: A): ConfigNode = {node.setValue(implicitly[TypeToken[A]], value); this}

		override def writeBoolean(value: Boolean): ConfigNode = writeValue(value)
		override def writeByte(value: Byte): ConfigNode = writeValue(value)
		override def writeShort(value: Short): ConfigNode = writeValue(value)
		override def writeInt(value: Int): ConfigNode = writeValue(value)
		override def writeLong(value: Long): ConfigNode = writeValue(value)
		override def writeFloat(value: Float): ConfigNode = writeValue(value)
		override def writeDouble(value: Double): ConfigNode = writeValue(value)
		override def writeString(value: String): ConfigNode = writeValue(value)
	}

	implicit def typeSerializerConvert[A](implicit serializer: Strict[ConfigSerializer[A]]) = new TypeSerializer[A] {
		override def serialize(`type`: TypeToken[_], obj: A, value: ConfigurationNode): Unit = {
			serializer.value.write(obj, value)
		}
		override def deserialize(`type`: TypeToken[_], value: ConfigurationNode): A = {
			serializer.value.read(value).getOrElse(throw new ObjectMappingException)
		}
	}

	def fromTypeSerializer[A](typeSerializer: TypeSerializer[A])(implicit typeToken: TypeToken[A]): ConfigSerializer[A] = new ConfigSerializer[A] {
		override def write(obj: A, value: ConfigNode): ConfigNode = {
			typeSerializer.serialize(typeToken, obj, value.asInstanceOf[ConfigurationNodeWrapper].node)
			value
		}
		override def read(value: ConfigNode): Try[A] = Try(typeSerializer.deserialize(typeToken, value.asInstanceOf[ConfigurationNodeWrapper].node))
	}
}