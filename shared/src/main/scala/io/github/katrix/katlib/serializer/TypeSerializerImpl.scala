package io.github.katrix.katlib.serializer

import java.lang.{Byte => JByte, Double => JDouble, Float => JFloat, Integer => JInt, Long => JLong, Short => JShort}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

import org.apache.commons.lang3.math.NumberUtils

import com.google.common.reflect.TypeToken

import io.github.katrix.katlib.serializer.ConfigSerializerBase.{ConfigNode, ConfigSerializer}
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer
import shapeless.Lazy

object TypeSerializerImpl {

	implicit class ConfigurationNodeWrapper(val node: ConfigurationNode) extends ConfigNode {

		override def getParent: ConfigNode = node.getParent
		override def getNode(string: String*): ConfigNode = node.getNode(string: _*)
		override def hasNode(string: String*): Boolean = node.getNode(string: _*).isVirtual
		override def getChildren: Seq[ConfigNode] = node.getChildrenList.asScala.map(new ConfigurationNodeWrapper(_))

		override def read[A: ConfigSerializer]: Try[A] = {
			val serializer = implicitly[ConfigSerializer[A]]
			serializer.shouldBypass match {
				case Some(clazz) => readValue(TypeToken.of(clazz))
				case None => serializer.read(this)
			}
		}

		def readValue[A](typeToken: TypeToken[A]): Try[A] = Try(node.getValue(typeToken))

		override def readList[A: ConfigSerializer]: Try[Seq[A]] = {
			def useMap: Try[Seq[A]] = read[Map[Int, A]].map(_.toSeq.sortBy(_._1).map(_._2))

			//TODO: Figure out how to check if the node supports lists
			/*
			implicitly[ConfigSerializer[A]].shouldBypass match {
				case Some(clazz) => useMap
				case None => useMap
			}
			*/
			useMap
		}

		override def write[A: ConfigSerializer](value: A): ConfigNode = {
			val serializer = implicitly[ConfigSerializer[A]]
			serializer.shouldBypass match {
				case Some(clazz) => writeValue(value, TypeToken.of(clazz))
				case None => serializer.write(value, this)
			}
		}

		def writeValue(value: AnyRef): ConfigNode = node.setValue(value)
		def writeValue[A](value: A, typeToken: TypeToken[A]): ConfigNode = node.setValue(typeToken, value)

		override def writeList[A: ConfigSerializer](value: Seq[A]): ConfigNode = {
			def useMap: ConfigNode = write[Map[Int, A]](value.zipWithIndex.map(_.swap).toMap)

			//TODO: Figure out how to check if the node supports lists
			/*
			implicitly[ConfigSerializer[A]].shouldBypass match {
				case Some(clazz) => useMap
				case None => useMap
			}
			*/
			useMap
		}
	}

	implicit def typeSerializer[A](implicit serializer: Lazy[ConfigSerializer[A]]) = new TypeSerializer[A] {
		override def serialize(`type`: TypeToken[_], obj: A, node: ConfigurationNode): Unit = {
			serializer.value.write(obj, node)
		}
		override def deserialize(`type`: TypeToken[_], node: ConfigurationNode): A = {
			serializer.value.read(node).getOrElse(throw new ObjectMappingException)
		}
	}

	def mapSerializer[A, B: ConfigSerializer](keyValidate: PartialFunction[AnyRef, A]) = new ConfigSerializer[Map[A, B]] {
		override def write(obj: Map[A, B], node: ConfigNode): ConfigNode = node.asInstanceOf[ConfigurationNodeWrapper].writeValue(obj.asJava)
		override def read(node: ConfigNode): Try[Map[A, B]] = {
			val configNode = node.asInstanceOf[ConfigurationNodeWrapper].node
			if(configNode.hasMapChildren) {
				val tryMap = configNode.getChildrenMap.asScala.map { case (k, v) => keyValidate.lift(k) -> v.read[B] }
				if(tryMap.forall { case (k, v) => k.isDefined && v.isSuccess }) {
					Success(Map(tryMap.map { case (k, v) => k.get -> v.get }.toSeq: _*))
				}
				else Failure(new ObjectMappingException)

			}
			else Failure(new ObjectMappingException)
		}
	}

	implicit def stringMapSerializer[A: ConfigSerializer]: ConfigSerializer[Map[String, A]] = mapSerializer[String, A] { case str: String => str }
	implicit def byteMapSerializer[A: ConfigSerializer]: ConfigSerializer[Map[Byte, A]] = mapSerializer[Byte, A] {
		case int: JByte => int.byteValue()
		case str: String if NumberUtils.isNumber(str) => str.toByte
	}
	implicit def shortMapSerializer[A: ConfigSerializer]: ConfigSerializer[Map[Short, A]] = mapSerializer[Short, A] {
		case int: JShort => int.shortValue()
		case str: String if NumberUtils.isNumber(str) => str.toShort
	}
	implicit def intMapSerializer[A: ConfigSerializer]: ConfigSerializer[Map[Int, A]] = mapSerializer[Int, A] {
		case int: JInt => int.intValue()
		case str: String if NumberUtils.isNumber(str) => str.toInt
	}
	implicit def longMapSerializer[A: ConfigSerializer]: ConfigSerializer[Map[Long, A]] = mapSerializer[Long, A] {
		case int: JLong => int.longValue()
		case str: String if NumberUtils.isNumber(str) => str.toLong
	}
	implicit def floatMapSerializer[A: ConfigSerializer]: ConfigSerializer[Map[Float, A]] = mapSerializer[Float, A] {
		case int: JFloat => int.floatValue()
		case str: String if NumberUtils.isNumber(str) => str.toFloat
	}
	implicit def doubleMapSerializer[A: ConfigSerializer]: ConfigSerializer[Map[Double, A]] = mapSerializer[Double, A] {
		case int: JDouble => int.doubleValue()
		case str: String if NumberUtils.isNumber(str) => str.toDouble
	}

	def fromTypeSerializer[A](typeSerializer: TypeSerializer[A], clazz: Class[A]): ConfigSerializer[A] = new ConfigSerializer[A] {
		private val typeToken = TypeToken.of(clazz)
		override def shouldBypass: Option[Class[A]] = Some(clazz)
		override def write(obj: A, node: ConfigNode): ConfigNode = {
			typeSerializer.serialize(typeToken, obj, node.asInstanceOf[ConfigurationNodeWrapper].node)
			node
		}
		override def read(node: ConfigNode): Try[A] = Try(typeSerializer.deserialize(typeToken, node.asInstanceOf[ConfigurationNodeWrapper].node))
	}
}