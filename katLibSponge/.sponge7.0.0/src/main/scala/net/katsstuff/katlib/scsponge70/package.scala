package net.katsstuff.katlib

import scala.reflect.ClassTag
import scala.collection.JavaConverters._
import scala.collection.mutable

import org.spongepowered.api.Sponge

import net.katsstuff.katlib.helper.Implicits._

package object scsponge70 {

  type CauseStackManager = org.spongepowered.api.event.CauseStackManager
  def CauseStackManager: CauseStackManager = Sponge.getCauseStackManager

  implicit class CauseStackManagerSyntax(private val csm: CauseStackManager) {

    def currentCause: Cause = csm.getCurrentCause

    def currentContext: EventContext = csm.getCurrentContext

    def getContext[A](key: EventContextKey[A]): Option[A] = csm.getContext(key).toOption

    def removeContext[A](key: EventContextKey[A]): Option[A] = csm.removeContext(key).toOption

    def usingStackFrame[A](f: StackFrame => A): A = {
      val frame = csm.pushCauseFrame()
      val res   = f(frame)
      frame.close()
      res
    }
  }

  type StackFrame = org.spongepowered.api.event.CauseStackManager.StackFrame
  implicit class StackFrameSyntax(private val stackFrame: StackFrame) extends AnyVal {

    def currentCause: Cause = stackFrame.getCurrentCause

    def currentContext: EventContext = stackFrame.getCurrentContext

    def removeContext[A](key: EventContextKey[A]): Option[A] = stackFrame.removeContext(key).toOption
  }

  type Cause = org.spongepowered.api.event.cause.Cause
  implicit class CauseSyntax(private val cause: Cause) extends AnyVal {
    def context: EventContext = cause.getContext

    def first[A](implicit classTag: ClassTag[A]): Option[A] =
      cause.first(classTag.getClass.asInstanceOf[Class[A]]).toOption

    def last[A](implicit classTag: ClassTag[A]): Option[A] =
      cause.last(classTag.getClass.asInstanceOf[Class[A]]).toOption

    def before[A](implicit classTag: ClassTag[A]): Option[Any] =
      cause.before(classTag.getClass.asInstanceOf[Class[A]]).toOption

    def after[A](implicit classTag: ClassTag[A]): Option[Any] =
      cause.after(classTag.getClass.asInstanceOf[Class[A]]).toOption

    def containsType[A](implicit classTag: ClassTag[A]): Boolean =
      cause.containsType(classTag.getClass.asInstanceOf[Class[A]])

    def allOf[A](implicit classTag: ClassTag[A]): mutable.Buffer[A] =
      cause.allOf(classTag.getClass.asInstanceOf[Class[A]]).asScala

    def noneOf[A](implicit classTag: ClassTag[A]): mutable.Buffer[AnyRef] =
      cause.noneOf(classTag.getClass.asInstanceOf[Class[A]]).asScala

    def all: mutable.Buffer[AnyRef] = cause.all.asScala
  }

  type EventContext       = org.spongepowered.api.event.cause.EventContext
  implicit class EventContextSyntax(private val context: EventContext) extends AnyVal {
    def get[A](key: EventContextKey[A]): Option[A] = context.get(key).toOption

    def keySet: mutable.Set[EventContextKey[_]] = context.keySet.asScala

    def asMap: mutable.Map[EventContextKey[_], AnyRef] = context.asMap.asScala
  }

  type EventContextKey[A] = org.spongepowered.api.event.cause.EventContextKey[A]
}
