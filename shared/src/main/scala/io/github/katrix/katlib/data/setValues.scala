package io.github.katrix.katlib.data

import scala.collection.immutable
import scala.collection.mutable

trait ImmutableSetValue[A] extends ImmutableIterableValue[A, immutable.Set[A]] {

	override type Self = ImmutableSetValue[A]
	override type MutableCollection <: mutable.Set[A]
	override type Mutable = SetValue[A]
}

trait SetValue[A] extends IterableValue[A, mutable.Set[A]] {

	override type Self = SetValue[A]
	override type ImmutableCollection <: mutable.Set[A]
	override type Immutable = SetValue[A]
}