package io.github.katrix.katlib.i18n

import java.util.Locale

import org.spongepowered.api.command.CommandSource

object Localized {
  val Default: Locale = Locale.getDefault
  def apply[A](src: CommandSource)(f: Locale => A): A = f(src.getLocale)
}