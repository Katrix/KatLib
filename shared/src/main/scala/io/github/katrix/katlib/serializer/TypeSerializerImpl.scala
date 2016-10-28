package io.github.katrix.katlib.serializer

import scala.collection.JavaConverters._
import scala.util.Try

import com.google.common.reflect.TypeToken

import io.github.katrix.katlib.serializer.ConfigSerializerBase.{ConfigNode, ConfigSerializer}
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer
import shapeless.Strict

object TypeSerializerImpl extends DefaultSerializersImpl {

	implicit class ConfigurationNodeWrapper(val node: ConfigurationNode) extends ConfigNode {

		override def getParent: ConfigNode = node.getParent
		override def getNode(string: String*): ConfigNode = node.getNode(string: _*)
		override def getChildren: Seq[ConfigNode] = node.getChildrenList.asScala.map(new ConfigurationNodeWrapper(_))

		override def read[A: ConfigSerializer]: Try[A] = {
			val serializer = implicitly[ConfigSerializer[A]]
			serializer.shouldBypass match {
				case Some(token) => readValue(token)
				case None => serializer.read(this)
			}
		}

		def readValue[A: TypeToken]: Try[A] = Try(node.getValue(implicitly[TypeToken[A]]))

		override def readList[A: ConfigSerializer]: Try[Seq[A]] = {
			implicitly[ConfigSerializer[A]].shouldBypass match {
				case Some(token) => Try(node.getList[A](token).asScala)
				case None => read[Map[Int, A]].map(_.toSeq.sortBy(_._1).map(_._2))
			}
		}

		override def write[A: ConfigSerializer](value: A): ConfigNode = {
			val serializer = implicitly[ConfigSerializer[A]]
			serializer.shouldBypass match {
				case Some(token) => writeValue(value)(token)
				case None => serializer.write(value, this)
			}
		}

		def writeValue(value: AnyRef): ConfigNode = node.setValue(value)
		def writeValue[A: TypeToken](value: A): ConfigNode = node.setValue(implicitly[TypeToken[A]], value)

		override def writeList[A: ConfigSerializer](value: Seq[A]): ConfigNode = {
			implicitly[ConfigSerializer[A]].shouldBypass match {
				case Some(_) => writeValue(value.asJava)
				case None => write[Map[Int, A]](value.zipWithIndex.map(_.swap).toMap)
			}
		}
	}

	implicit def typeSerializerConvert[A](implicit serializer: Strict[ConfigSerializer[A]]) = new TypeSerializer[A] {
		override def serialize(`type`: TypeToken[_], obj: A, value: ConfigurationNode): Unit = {
			serializer.value.write(obj, value)
		}
		override def deserialize(`type`: TypeToken[_], value: ConfigurationNode): A = {
			serializer.value.read(value).getOrElse(throw new ObjectMappingException)
		}
	}

	def fromTypeSerializer[A](typeSerializer: TypeSerializer[A], bypass: Boolean)(implicit typeToken: TypeToken[A]): ConfigSerializer[A]
	= new ConfigSerializer[A] {
		override def shouldBypass: Option[TypeToken[A]] = if(bypass) Some(typeToken) else None
		override def write(obj: A, value: ConfigNode): ConfigNode = {
			typeSerializer.serialize(typeToken, obj, value.asInstanceOf[ConfigurationNodeWrapper].node)
			value
		}
		override def read(value: ConfigNode): Try[A] = Try(typeSerializer.deserialize(typeToken, value.asInstanceOf[ConfigurationNodeWrapper].node))
	}
}