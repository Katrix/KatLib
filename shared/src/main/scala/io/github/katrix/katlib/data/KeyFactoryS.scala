package io.github.katrix.katlib.data

import scala.reflect.ClassTag

import org.spongepowered.api.data.DataQuery
import org.spongepowered.api.data.key.Key
import org.spongepowered.api.data.value.BaseValue

import com.google.common.base.Objects

object KeyFactoryS {

	def makeSingleKey[A : ClassTag, B <: BaseValue[A] : ClassTag](query: DataQuery): Key[B] = new Key[B] {
		private val hash: Int = Objects.hashCode(implicitly[ClassTag[A]].runtimeClass, implicitly[ClassTag[B]].runtimeClass, query)

		def getValueClass: Class[B] = implicitly[ClassTag[B]].runtimeClass.asInstanceOf[Class[B]]
		def getQuery: DataQuery = query
		override def hashCode: Int = this.hash
		override def toString: String = s"Key{Value:${implicitly[ClassTag[B]].runtimeClass.getSimpleName}<${
			implicitly[ClassTag[A]].runtimeClass.getSimpleName}>, Query: ${query.toString}}"
	}

	def makeSeqKey[A : ClassTag](query: DataQuery): Key[SeqValue[A]] = new Key[SeqValue[A]] {
		private val hash = Objects.hashCode(classOf[SeqValue[_]], implicitly[ClassTag[A]].runtimeClass, query)

		override def getQuery: DataQuery = query
		override def getValueClass: Class[SeqValue[A]] = classOf[SeqValue[_]].asInstanceOf[Class[SeqValue[A]]]

		override def hashCode: Int = hash
		override def toString: String = s"Key{Value:SeqValue<${implicitly[ClassTag[A]].runtimeClass.getSimpleName}>, Query: ${query.toString}}"
	}

	def makeSetKey[A : ClassTag](query: DataQuery): Key[SetValue[A]] = new Key[SetValue[A]] {
		private val hash = Objects.hashCode(classOf[SetValue[_]], implicitly[ClassTag[A]].runtimeClass, query)

		override def getQuery: DataQuery = query
		override def getValueClass: Class[SetValue[A]] = classOf[SetValue[_]].asInstanceOf[Class[SetValue[A]]]

		override def hashCode: Int = hash
		override def toString: String = s"Key{Value:SetValue<${implicitly[ClassTag[A]].runtimeClass.getSimpleName}>, Query: ${query.toString}}"
	}

	def makeOptionKey[A : ClassTag](query: DataQuery): Key[OptionValue[A]] = new Key[OptionValue[A]] {
		private val hash = Objects.hashCode(classOf[OptionValue[_]], implicitly[ClassTag[A]].runtimeClass, query)

		override def getQuery: DataQuery = query
		override def getValueClass: Class[OptionValue[A]] = classOf[OptionValue[_]].asInstanceOf[Class[OptionValue[A]]]

		override def hashCode: Int = hash
		override def toString: String = s"Key{Value:OptionValue<${implicitly[ClassTag[A]].runtimeClass.getSimpleName}>, Query: ${query.toString}}"
	}

}
