package io.github.katrix.katlib.data

import scala.reflect.ClassTag

import org.spongepowered.api.data.DataQuery
import org.spongepowered.api.data.key.{Key, KeyFactory}
import org.spongepowered.api.data.value.BaseValue
import org.spongepowered.api.data.value.mutable.{ListValue, MapValue, OptionalValue, SetValue}

object KeyFactoryS {

	def makeSingleKey[A : ClassTag, B <: BaseValue[A] : ClassTag](query: DataQuery): Key[B] = {
		KeyFactory.makeSingleKey(
			implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]],
			implicitly[ClassTag[B]].runtimeClass.asInstanceOf[Class[B]],
			query)
	}

	def makeListKey[A : ClassTag](query: DataQuery): Key[ListValue[A]] = {
		KeyFactory.makeListKey(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]], query)
	}

	def makeSetKey[A : ClassTag](query: DataQuery): Key[SetValue[A]] = {
		KeyFactory.makeSetKey(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]], query)
	}

	def makeOptionalKey[A : ClassTag](query: DataQuery): Key[OptionalValue[A]] = {
		KeyFactory.makeOptionalKey(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]], query)
	}

	def makeMapKey[A : ClassTag, B : ClassTag](query: DataQuery): Key[MapValue[A, B]] = {
		KeyFactory.makeMapKey(
			implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]],
			implicitly[ClassTag[B]].runtimeClass.asInstanceOf[Class[B]],
			query
		)
	}

}
