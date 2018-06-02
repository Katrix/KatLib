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
package net.katsstuff.katlib.helper

import java.util.Optional

import scala.language.experimental.macros
import scala.reflect.ClassTag
import scala.reflect.macros.blackbox
import scala.util.Try

import org.spongepowered.api.data.persistence.{DataBuilder, DataContentUpdater}
import org.spongepowered.api.data.{DataManager, DataSerializable, DataView, ImmutableDataBuilder, ImmutableDataHolder}
import org.spongepowered.api.service.{ProviderRegistration, ServiceManager}

import com.google.common.reflect.TypeToken

object Implicits extends SpongeProtocol {

  implicit class RichOptional[A](private val optional: Optional[A]) extends AnyVal {

    def toOption: Option[A] =
      if (optional.isPresent) {
        Some(optional.get())
      } else {
        None
      }
  }

  implicit class RichOption[A](private val option: Option[A]) extends AnyVal {

    def toOptional: Optional[A] =
      option match {
        case Some(value) => Optional.of(value)
        case None        => Optional.empty()
      }
  }

  implicit def typeToken[A]: TypeToken[A] = macro MacroImpl.typeToken[A]

  implicit class RichDataManager(private val dataManager: DataManager) extends AnyVal {

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

  implicit class RichServiceManager(private val serviceManager: ServiceManager) extends AnyVal {

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
