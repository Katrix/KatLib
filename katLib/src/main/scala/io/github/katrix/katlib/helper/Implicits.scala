/*
 * This file is part of KatLib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 Katrix
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.katrix.katlib.helper

import java.nio.file.Path
import java.util.Optional

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.language.experimental.macros
import scala.reflect.ClassTag
import scala.reflect.macros.blackbox
import scala.util.Try

import org.slf4j.Logger
import org.spongepowered.api.asset.Asset
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.data.persistence.{DataBuilder, DataContentUpdater}
import org.spongepowered.api.data.{DataManager, DataSerializable, DataView, ImmutableDataBuilder, ImmutableDataHolder}
import org.spongepowered.api.plugin.{PluginContainer => SpongePluginContainer}
import org.spongepowered.api.service.{ProviderRegistration, ServiceManager}
import org.spongepowered.api.text.format.{TextColor, TextColors, TextStyle}
import org.spongepowered.api.text.{Text, TextTemplate}

import com.google.common.reflect.TypeToken

import ninja.leaping.configurate.ConfigurationNode

object Implicits {

  implicit class RichString(val string: String) extends AnyVal {

    def text: Text = Text.of(string)
  }

  implicit class RichOptional[A](val optional: Optional[A]) extends AnyVal {

    def toOption: Option[A] =
      if (optional.isPresent) {
        Some(optional.get())
      } else {
        None
      }
  }

  implicit class RichOption[A](val option: Option[A]) extends AnyVal {

    def toOptional: Optional[A] =
      option match {
        case Some(value) => Optional.of(value)
        case None        => Optional.empty()
      }
  }

  implicit class PluginContainer(val container: SpongePluginContainer) extends AnyVal {

    def id:                     String         = container.getId
    def name:                   String         = container.getName
    def version:                Option[String] = container.getVersion.toOption
    def description:            Option[String] = container.getDescription.toOption
    def url:                    Option[String] = container.getUrl.toOption
    def authors:                Seq[String]    = container.getAuthors.asScala
    def getAsset(name: String): Option[Asset]  = container.getAsset(name).toOption
    def sources:                Option[Path]   = container.getSource.toOption
    def instance:               Option[_]      = container.getInstance().toOption
    def logger:                 Logger         = container.getLogger
  }

  implicit def typeToken[A]: TypeToken[A] = macro MacroImpl.typeToken[A]

  implicit class TextStringContext(private val sc: StringContext) extends AnyVal {

    /**
			* Create a [[Text]] representation of this string.
			* Really just a nicer way of saying [[Text#of(anyRef: AnyRef*]]
			*/
    def t(args: Any*): Text = {
      sc.checkLengths(args)

      @tailrec
      def inner(partsLeft: Seq[String], argsLeft: Seq[Any], res: Seq[AnyRef]): Seq[AnyRef] =
        if (argsLeft == Nil) res
        else {
          inner(partsLeft.tail, argsLeft.tail, (res :+ argsLeft.head.asInstanceOf[AnyRef]) :+ partsLeft.head)
        }

      Text.of(inner(sc.parts.tail, args, Seq(sc.parts.head)): _*)
    }

    /**
			* Create a [[Text]] representation of this string.
			* String arguments are converted into [[TextTemplate.Arg]]s
			* Really just a nicer way of saying [[TextTemplate#of(anyRef: AnyRef*]]
			*/
    def tt(args: Any*): TextTemplate = {
      sc.checkLengths(args)

      @tailrec
      def inner(partsLeft: Seq[String], argsLeft: Seq[Any], res: Seq[AnyRef]): Seq[AnyRef] =
        if (argsLeft == Nil) res
        else {
          val argObj = argsLeft.head match {
            case string: String => TextTemplate.arg(string)
            case any @ _        => any.asInstanceOf[AnyRef]
          }
          inner(partsLeft.tail, argsLeft.tail, (res :+ argObj) :+ partsLeft.head)
        }

      TextTemplate.of(inner(sc.parts.tail, args, Seq(sc.parts.head)): _*)
    }
  }

  implicit class RichDataManager(val dataManager: DataManager) extends AnyVal {

    def registerBuilder[A <: DataSerializable: ClassTag](builder: DataBuilder[A]): Unit =
      dataManager.registerBuilder(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]], builder)

    def registerContentUpdater[A <: DataSerializable: ClassTag](updater: DataContentUpdater): Unit =
      dataManager.registerContentUpdater(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]], updater)

    def getWrappedContentUpdater[A <: DataSerializable: ClassTag](from: Int, to: Int): Option[DataContentUpdater] =
      dataManager
        .getWrappedContentUpdater(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]], from, to)
        .toOption

    def getBuilder[A <: DataSerializable: ClassTag]: Option[DataBuilder[A]] =
      dataManager.getBuilder(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]]).toOption

    def deserialize[A <: DataSerializable: ClassTag](dataView: DataView): Option[A] =
      dataManager.deserialize(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]], dataView).toOption

    def register[T <: ImmutableDataHolder[T]: ClassTag, B <: ImmutableDataBuilder[T, B]](builder: B): Unit =
      dataManager.register(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]], builder)

    def getImmutableBuilder[A <: ImmutableDataHolder[A]: ClassTag, B <: ImmutableDataBuilder[A, B]]: Option[B] =
      dataManager.getImmutableBuilder(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]]).toOption
  }

  implicit class RichServiceManager(val serviceManager: ServiceManager) extends AnyVal {

    def provide[A: ClassTag]: Option[A] =
      serviceManager.provide(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]]).toOption
    def provideTry[A: ClassTag]: Try[A] =
      Try(serviceManager.provideUnchecked(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]]))
    def provideUnchecked[A: ClassTag]: A =
      serviceManager.provideUnchecked(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]])

    def isRegistered[A: ClassTag]: Boolean =
      serviceManager.isRegistered(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]])

    def getRegistration[A: ClassTag]: Option[ProviderRegistration[A]] =
      serviceManager.getRegistration(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]]).toOption
  }
}

class MacroImpl(val c: blackbox.Context) {
  import c.universe._
  import definitions.NothingClass

  def checkType(tpe: Type): Unit = tpe.dealias match {
    case t: TypeRef if t.sym == NothingClass =>
      c.abort(c.enclosingPosition, "No TypeToken for Nothing")
    case t if !t.typeSymbol.isClass => c.abort(c.enclosingPosition, "The type must be concrete")
    case t if t.typeArgs.nonEmpty   => t.typeArgs.foreach(t => checkType(t))
    case _                          =>
  }

  def typeToken[A: c.WeakTypeTag]: c.Expr[TypeToken[A]] = {
    val tpe = implicitly[c.WeakTypeTag[A]].tpe

    checkType(tpe)

    val tree = if (tpe.typeArgs.nonEmpty) {
      q"new _root_.com.google.common.reflect.TypeToken[$tpe] {}"
    } else {
      q"_root_.com.google.common.reflect.TypeToken.of(classOf[$tpe])"
    }

    c.Expr[TypeToken[A]](tree)
  }

}
