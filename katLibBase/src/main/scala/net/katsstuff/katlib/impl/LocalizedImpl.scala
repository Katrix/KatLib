package net.katsstuff.katlib.impl

import java.util.Locale

import cats.{FlatMap, ~>}
import cats.syntax.flatMap._
import net.katsstuff.katlib.algebras.{CommandSources, Localized}

class LocalizedImpl[F[_]: FlatMap, CommandSource](implicit CS: CommandSources[F, CommandSource])
    extends Localized[F, CommandSource] { self =>
  override def apply[A](src: CommandSource)(f: Locale => F[A]): F[A] = CS.locale(src).flatMap(f)

  def mapK[G[_]: FlatMap](f: F ~> G): Localized[G, CommandSource] = new MappedLocalized(f)
}

class MappedLocalized[F[_], G[_]: FlatMap, CommandSource](f: F ~> G)(implicit CS: CommandSources[F, CommandSource])
    extends Localized[G, CommandSource] {
  override def apply[A](src: CommandSource)(g: Locale => G[A]): G[A] = f(CS.locale(src)).flatMap(g)

  override def mapK[H[_]: FlatMap](g: G ~> H): Localized[H, CommandSource] = new MappedLocalized(f.andThen(g))
}
