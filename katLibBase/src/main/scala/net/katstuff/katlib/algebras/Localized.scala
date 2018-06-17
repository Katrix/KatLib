package net.katstuff.katlib.algebras

import java.util.Locale

import cats.{FlatMap, ~>}

/**
  * Provides a way to run a computation with a locale of a command source.
  */
trait Localized[F[_], CommandSource] {

  def apply[A](src: CommandSource)(f: Locale => F[A]): F[A]

  def mapK[G[_]: FlatMap](f: F ~> G): Localized[G, CommandSource]
}
