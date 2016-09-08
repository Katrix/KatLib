package io.github.katrix.katlib.data

import java.util.Optional
import java.util.function.Function

import scala.collection.immutable
import scala.collection.mutable

import io.github.katrix.katlib.helper.Implicits.RichOption
import org.spongepowered.api.data.value.immutable.ImmutableValue
import org.spongepowered.api.data.value.mutable.Value

//Some copying from the Sponge Collection Values, especially documentation
trait ImmutableIterableValue[B, A <: immutable.Iterable[B]] extends ImmutableValue[A] {

	/**
		* The type of this
		*/
	type Self <: ImmutableIterableValue[B, A]

	/**
		* The type of the collection of the mutable value
		*/
	type MutableCollection <: mutable.Iterable[B]

	/**
		* The type of the mutable value
		*/
	type Mutable <: IterableValue[B, MutableCollection]

	/**
		* Gets the size of the underlying collection of elements.
		*
		* @return The size
		*/
	def size: Int = get.size

	/**
		* Checks if the backed [[Iterable]] is empty.
		*
		* @see [[Iterable#isEmpty]]
		* @return True if the collection is empty
		*/
	def isEmpty: Boolean = get.isEmpty

	/**
		* Adds a single element to the underlying [[Iterable]]
		*/
	def +(elem: B): Self

	/**
		* Adds a several new elements to the underlying [[Iterable]]
		*/
	def ++(elements: Iterable[B]): Self

	def map(f: A => A): Self
	def flatMap(f: A => Self): Self = f(get)
	def filter(f: A => Boolean): Self

	/**
		* A convenience method to allow working withe the Scala
		* library more.
		*/
	def getOption: Option[A]

	/**
		* An alias of [[`with`]] as with is a reserved keyword
		* in Scala and can be annoying to type.
		*/
	def set(value: A): Self

	override def `with`(value: A): Self = set(value)
	override def getDirect: Optional[A] = getOption.toOptional
	override def transform(function: Function[A, A]): Self = map(function.apply)
	override def asMutable(): Mutable
}

trait IterableValue[B, A <: mutable.Iterable[B]] extends Value[A] {

	/**
		* The type of this
		*/
	type Self <: IterableValue[B, A]

	/**
		* The type of the collection of the immutable value
		*/
	type ImmutableCollection <: immutable.Iterable[B]

	/**
		* The type of the immutable value
		*/
	type Immutable <: ImmutableIterableValue[B, ImmutableCollection]

	/**
		* Gets the size of the underlying collection of elements.
		*
		* @return The size
		*/
	def size: Int

	/**
		* Checks if the backed [[Iterable]] is empty.
		*
		* @see [[Iterable#isEmpty]]
		* @return True if the collection is empty
		*/
	def isEmpty: Boolean

	def +=(elem: B): Self
	def ++=(elements: Iterable[B]): Self

	def -=(elem: B): Self
	def --=(elements: Iterable[B]): Self

	def map(f: A => A): Self
	def flatMap(f: A => Self): Self
	def filter(f: A => Boolean): Self

	def immutableCopy: ImmutableCollection

	/**
		* A convenience method to allow working withe the Scala
		* library more.
		*/
	def getOption: Option[A]

	override def set(value: A): Self
	override def getDirect: Optional[A] = getOption.toOptional
	override def transform(function: Function[A, A]): Self = map(function.apply)
	override def asImmutable(): Immutable
}