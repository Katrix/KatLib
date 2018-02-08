package io.github.katrix.katlib.helper

import scala.reflect.ClassTag

import io.github.katrix.katlib.helper.Implicits._

import org.spongepowered.api.data.DataManager
import org.spongepowered.api.data.persistence.DataTranslator

object Implicits700 {

  implicit class RichDataManager(val dataManager: DataManager) extends AnyVal {

    def registerTranslator[A: ClassTag](serializer: DataTranslator[A]): Unit =
      dataManager.registerTranslator(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]], serializer)

    def getTranslator[A: ClassTag]: Option[DataTranslator[A]] =
      dataManager.getTranslator(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]]).toOption
  }

}
