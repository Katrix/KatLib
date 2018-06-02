package net.katstuff.katlib.impl

import java.util.Locale

import cats.FlatMap
import cats.syntax.flatMap._
import net.katstuff.katlib.algebras.{CommandSourceAccess, Localized}

class LocalizedImpl[F[_]: FlatMap, CommandSource](implicit CS: CommandSourceAccess[F, CommandSource])
    extends Localized[F, CommandSource] {
  override def apply[A](src: CommandSource)(f: Locale => F[A]): F[A] = CS.locale(src).flatMap(f)
}
