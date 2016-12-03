package io.github.katrix.katlib.serializer

import java.lang.{Byte => JByte, Double => JDouble, Float => JFloat, Integer => JInt, Long => JLong, Short => JShort}
import java.util

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

import org.apache.commons.lang3.math.NumberUtils

import com.google.common.reflect.TypeToken

import io.github.katrix.katlib.serializer.ConfigSerializerBase.{ConfigNode, ConfigSerializer}
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer
import ninja.leaping.configurate.{ConfigurationNode, SimpleConfigurationNode}
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

		def readValue[A](typeToken: TypeToken[A]): Try[A] = Try(Option(node.getValue(typeToken)).getOrElse(throw new ObjectMappingException))

		override def readList[A: ConfigSerializer]: Try[Seq[A]] = {
			read[Map[Int, A]].map(_.toSeq.sortBy(_._1).map(_._2))
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
			write[Map[Int, A]](value.zipWithIndex.map(_.swap).toMap)
		}
	}

	implicit def typeSerializer[A](implicit serializer: Lazy[ConfigSerializer[A]]) = new TypeSerializer[A] {
		override def serialize(`type`: TypeToken[_], obj: A, node: ConfigurationNode): Unit = {
			serializer.value.write(obj, node)
		}
		override def deserialize(`type`: TypeToken[_], node: ConfigurationNode): A = {
			serializer.value.read(node).recoverWith {
				case e: ObjectMappingException => Failure(e)
				case e => Failure(new ObjectMappingException(e))
			}.get
		}
	}

	def mapSerializer[A, B: ConfigSerializer](keyValidate: PartialFunction[AnyRef, A]): ConfigSerializer[Map[A, B]] = {
		mapSerializer[A, B, A](keyValidate)(identity)
	}

	def mapSerializer[A, B: ConfigSerializer, C](keyValidate: PartialFunction[AnyRef, A])(keyFun: A => C) = new ConfigSerializer[Map[A, B]] {
		import scala.language.implicitConversions
		implicit def toConfiguration(node: ConfigNode): ConfigurationNode = node.asInstanceOf[ConfigurationNodeWrapper].node

		override def write(obj: Map[A, B], node: ConfigNode): ConfigNode = {
			val serializer = implicitly[ConfigSerializer[B]]
			val toWrite: util.Map[C, ConfigurationNode] = obj.map{
				case (k, v) =>
					val value: ConfigurationNode = SimpleConfigurationNode.root().write(v)
					keyFun(k) -> value
			}.asJava
			node.setValue(toWrite)
		}

		override def read(node: ConfigNode): Try[Map[A, B]] = {
			val configNode: ConfigurationNode = node
			if(configNode.hasMapChildren) {
				val tryMap = configNode.getChildrenMap.asScala.map { case (k, v) => keyValidate.lift(k) -> v.read[B] }

				//The TypeSerializer for maps are lenient with errors so we are too
				val successes = tryMap.collect {
					case (Some(k), Success(v)) => k -> v
				}
				Success(Map(successes.toSeq: _*))
			}
			else Success(Map())
		}
	}

	implicit def stringMapSerializer[A: ConfigSerializer]: ConfigSerializer[Map[String, A]] = mapSerializer[String, A] { case str: String => str }
	implicit def byteMapSerializer[A: ConfigSerializer]: ConfigSerializer[Map[Byte, A]] = mapSerializer[Byte, A, JByte] {
		case byte: JByte => byte.byteValue()
		case str: String if NumberUtils.isNumber(str) => str.toByte
	} (Byte.box)
	implicit def shortMapSerializer[A: ConfigSerializer]: ConfigSerializer[Map[Short, A]] = mapSerializer[Short, A, JShort] {
		case short: JShort => short.shortValue()
		case str: String if NumberUtils.isNumber(str) => str.toShort
	} (Short.box)
	implicit def intMapSerializer[A: ConfigSerializer]: ConfigSerializer[Map[Int, A]] = mapSerializer[Int, A, JInt] {
		case int: JInt => int.intValue()
		case str: String if NumberUtils.isNumber(str) => str.toInt
	} (Int.box)
	implicit def longMapSerializer[A: ConfigSerializer]: ConfigSerializer[Map[Long, A]] = mapSerializer[Long, A, JLong] {
		case long: JLong => long.longValue()
		case str: String if NumberUtils.isNumber(str) => str.toLong
	} (Long.box)
	implicit def floatMapSerializer[A: ConfigSerializer]: ConfigSerializer[Map[Float, A]] = mapSerializer[Float, A, JFloat] {
		case float: JFloat => float.floatValue()
		case str: String if NumberUtils.isNumber(str) => str.toFloat
	} (Float.box)
	implicit def doubleMapSerializer[A: ConfigSerializer]: ConfigSerializer[Map[Double, A]] = mapSerializer[Double, A, JDouble] {
		case double: JDouble => double.doubleValue()
		case str: String if NumberUtils.isNumber(str) => str.toDouble
	} (Double.box)

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