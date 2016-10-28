package io.github.katrix.katlib.serializer

import scala.util.Try

import com.google.common.reflect.TypeToken

import io.github.katrix.katlib.helper.Implicits.typeToken
import io.github.katrix.katlib.serializer.CaseSerializers.{ConfigNode, TypeClassSerializer}
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer
import shapeless.Strict

object TypeSerializerImpl extends DefaultSerializers {

	implicit class ConfigurationNodeWrapper(node: ConfigurationNode) extends ConfigNode {

		override def getParent: ConfigNode = new ConfigurationNodeWrapper(node.getParent)
		override def getNode(string: String*): ConfigNode = new ConfigurationNodeWrapper(node.getNode(string: _*))

		override def readBoolean: Try[Boolean] = Try(node.getValue(typeToken[Boolean]))
		override def readByte: Try[Byte] = Try(node.getValue(typeToken[Byte]))
		override def readShort: Try[Short] = Try(node.getValue(typeToken[Short]))
		override def readInt: Try[Int] = Try(node.getValue(typeToken[Int]))
		override def readLong: Try[Long] = Try(node.getValue(typeToken[Long]))
		override def readFloat: Try[Float] = Try(node.getValue(typeToken[Float]))
		override def readDouble: Try[Double] = Try(node.getValue(typeToken[Double]))
		override def readString: Try[String] = Try(node.getValue(typeToken[String]))

		override def writeBoolean(value: Boolean): ConfigNode = {node.setValue(value); this}
		override def writeByte(value: Byte): ConfigNode = {node.setValue(value); this}
		override def writeShort(value: Short): ConfigNode = {node.setValue(value); this}
		override def writeInt(value: Int): ConfigNode = {node.setValue(value); this}
		override def writeLong(value: Long): ConfigNode = {node.setValue(value); this}
		override def writeFloat(value: Float): ConfigNode = {node.setValue(value); this}
		override def writeDouble(value: Double): ConfigNode = {node.setValue(value); this}
		override def writeString(value: String): ConfigNode = {node.setValue(value); this}
	}

	implicit def typeSerializerConvert[A](implicit serializer: Strict[TypeClassSerializer[A]]) = new TypeSerializer[A] {
		override def serialize(`type`: TypeToken[_], obj: A, value: ConfigurationNode): Unit = {
			serializer.value.write(obj, value)
		}
		override def deserialize(`type`: TypeToken[_], value: ConfigurationNode): A = {
			serializer.value.read(value).getOrElse(throw new ObjectMappingException)
		}
	}
}