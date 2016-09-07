package io.github.katrix.katlib.data

import org.spongepowered.api.data.value.immutable.ImmutableValue
import org.spongepowered.api.data.value.mutable.Value

trait ImmutableOptionValue[A] extends ImmutableValue[Option[A]] {

	def or(elem: A): Value[A]
}

trait OptionValue[A] extends Value[Option[A]] {

	def or(elem: A): Value[A]
}