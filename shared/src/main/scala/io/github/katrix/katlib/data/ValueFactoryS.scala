package io.github.katrix.katlib.data

import org.spongepowered.api.data.key.Key

object ValueFactoryS {

	def createSeqValue[A](key: Key[SeqValue[A]], elements: Seq[A]): SeqValue[A] = ???
	def createSeqValue[A](key: Key[SeqValue[A]], elements: Seq[A], defaults: Seq[A]): SeqValue[A] = ???

	def createSetValue[A](key: Key[SetValue[A]], elements: Set[A]): SetValue[A] = ???
	def createSetValue[A](key: Key[SetValue[A]], elements: Set[A], defaults: Seq[A]): SetValue[A] = ???

	def createMapValue[A, B](key: Key[Nothing[A, B]], elements: Map[A, B]): Nothing[A, B] = ???
	def createMapValue[A, B](key: Key[Nothing[A, B]], elements: Map[A, B], defaults: Map[A, B]): Nothing[A, B] = ???

	def createOptionValue[A](key: Key[OptionValue[A]], element: Option[A]): OptionValue[A] = ???
	def createOptionValue[A](key: Key[OptionValue[A]], element: Option[A], default: Option[A]): OptionValue[A] = ???
}
