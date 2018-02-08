package io.github.katrix.katlib.serializer

import scala.util.{Failure, Success, Try}

import shapeless._
import shapeless.labelled.{field, FieldType}

object ConfigSerializerBase {

  //We hide the implementation behind these traits so that it's easier to use for other stuff, in addition to implicits
  trait ConfigNode {
    def getParent:                ConfigNode
    def getNode(string: String*): ConfigNode
    def hasNode(string: String*): Boolean
    def getChildren:              Seq[ConfigNode]

    def read[A: ConfigSerializer]:            Try[A]
    def write[A: ConfigSerializer](value: A): ConfigNode

    def readList[A: ConfigSerializer]:                 Try[Seq[A]]
    def writeList[A: ConfigSerializer](value: Seq[A]): ConfigNode
  }

  trait ConfigSerializer[A] {
    def shouldBypass: Option[Class[A]] = None
    def write(obj: A, node: ConfigNode): ConfigNode
    def read(node: ConfigNode):          Try[A]
  }

  object ConfigSerializer extends DefaultSerializers with CaseClassSerializer {

    def apply[A](implicit configSerializer: ConfigSerializer[A]): ConfigSerializer[A] = configSerializer
  }

  trait DefaultSerializers {

    private def primitiveSerializer[A](clazz: Class[A]) = new ConfigSerializer[A] {
      override def shouldBypass:                    Option[Class[A]] = Some(clazz)
      override def write(obj: A, node: ConfigNode): ConfigNode       = node.write(obj)(this)
      override def read(node: ConfigNode):          Try[A]           = node.read(this)
    }

    implicit val booleanSerializer: ConfigSerializer[Boolean] = primitiveSerializer(classOf[Boolean])
    implicit val byteSerializer:    ConfigSerializer[Byte]    = primitiveSerializer(classOf[Byte])
    implicit val shortSerializer:   ConfigSerializer[Short]   = primitiveSerializer(classOf[Short])
    implicit val intSerializer:     ConfigSerializer[Int]     = primitiveSerializer(classOf[Int])
    implicit val longSerializer:    ConfigSerializer[Long]    = primitiveSerializer(classOf[Long])
    implicit val floatSerializer:   ConfigSerializer[Float]   = primitiveSerializer(classOf[Float])
    implicit val doubleSerializer:  ConfigSerializer[Double]  = primitiveSerializer(classOf[Double])
    implicit val stringSerializer:  ConfigSerializer[String]  = primitiveSerializer(classOf[String])

    implicit def seqSerializer[A: ConfigSerializer]: ConfigSerializer[Seq[A]] = new ConfigSerializer[Seq[A]] {
      override def write(obj: Seq[A], node: ConfigNode): ConfigNode  = node.writeList(obj)
      override def read(node: ConfigNode):               Try[Seq[A]] = node.readList[A]
    }
  }

  trait CaseClassSerializer {

    implicit def hNilSerializer: ConfigSerializer[HNil] = new ConfigSerializer[HNil] {
      override def write(obj: HNil, node: ConfigNode): ConfigNode = node
      override def read(node: ConfigNode):             Try[HNil]  = Success(HNil)
    }

    implicit def hListSerializer[Name <: Symbol, Head, Tail <: HList](
        implicit key: Witness.Aux[Name],
        sh: Lazy[ConfigSerializer[Head]],
        st: Lazy[ConfigSerializer[Tail]]
    ): ConfigSerializer[FieldType[Name, Head] :: Tail] = new ConfigSerializer[FieldType[Name, Head] :: Tail] {

      override def write(hList: FieldType[Name, Head] :: Tail, node: ConfigNode): ConfigNode = {
        val tailValue = st.value.write(hList.tail, node)
        sh.value.write(hList.head, tailValue.getNode(key.value.name)).getParent
      }

      override def read(node: ConfigNode): Try[FieldType[Name, Head] :: Tail] = {
        val head = sh.value.read(node.getNode(key.value.name))
        val tail = st.value.read(node)

        for {
          h <- head
          t <- tail
        } yield field[Name](h) :: t
      }
    }

    implicit object CNilSerializer extends ConfigSerializer[CNil] {
      override def write(obj: CNil, node: ConfigNode): ConfigNode = throw new IllegalStateException
      override def read(node: ConfigNode):             Try[CNil]  = throw new IllegalStateException
    }

    implicit def coProductSerializer[Name <: Symbol, Head, Tail <: Coproduct](
        implicit key: Witness.Aux[Name],
        sh: Lazy[ConfigSerializer[Head]],
        st: Lazy[ConfigSerializer[Tail]],
        strSer: ConfigSerializer[String]
    ): ConfigSerializer[FieldType[Name, Head] :+: Tail] = new ConfigSerializer[FieldType[Name, Head] :+: Tail] {

      override def write(obj: FieldType[Name, Head] :+: Tail, node: ConfigNode): ConfigNode = obj match {
        case Inl(found) =>
          val typeNode = strSer.write(key.value.name, node.getNode("type")).getParent
          sh.value.write(found, typeNode)
        case Inr(tail) => st.value.write(tail, node)
      }

      override def read(node: ConfigNode): Try[FieldType[Name, Head] :+: Tail] = {
        strSer.read(node.getNode("type")) match {
          case Success(tpe) =>
            if (tpe == key.value.name)
              sh.value.read(node).map(h => Inl(field[Name](h)))
            else
              st.value.read(node).map(t => Inr(t))

          case Failure(e) => Failure(e)
        }
      }
    }

    implicit def caseSerializer[A, Repr](
        implicit gen: LabelledGeneric.Aux[A, Repr],
        ser: Lazy[ConfigSerializer[Repr]]
    ): ConfigSerializer[A] =
      new ConfigSerializer[A] {
        override def write(obj: A, node: ConfigNode): ConfigNode = ser.value.write(gen.to(obj), node)
        override def read(node: ConfigNode):          Try[A]     = ser.value.read(node).map(gen.from)
      }
  }
}
