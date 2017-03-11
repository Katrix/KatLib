package io.github.katrix.katlib.helper

import scala.reflect.ClassTag

import io.github.katrix.katlib.helper.Implicits._

import org.spongepowered.api.data.DataManager
import org.spongepowered.api.data.persistence.DataSerializer

object Implicits410 {

  implicit class RichDataManager(val dataManager: DataManager) extends AnyVal {

    def registerSerializer[A: ClassTag](serializer: DataSerializer[A]): Unit =
      dataManager.registerSerializer(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]], serializer)

    def getSerializer[A: ClassTag]: Option[DataSerializer[A]] =
      dataManager.getSerializer(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]]).toOption
  }

}
