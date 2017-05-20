package io.github.katrix.katlib.persistant

import scala.collection.JavaConverters._
import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds
import scala.reflect.ClassTag

import com.google.common.collect.{ImmutableList, ImmutableMap}
import com.google.common.reflect.TypeToken

import ninja.leaping.configurate.objectmapping.ObjectMappingException
import ninja.leaping.configurate.objectmapping.serialize.{TypeSerializer, TypeSerializers}
import ninja.leaping.configurate.{ConfigurationNode, SimpleConfigurationNode}

object KatLibTypeSerializers {

  def registerScalaSerializers(): Unit = {
    val serializers = TypeSerializers.getDefaultSerializers

    def registerSerializer[Type](serializer: TypeSerializer[Type], typeToken: TypeToken[Type]): Unit = {
      serializers.registerType(typeToken, serializer)
    }

    registerSerializer(iterableSerializer[Iterable], new TypeToken[Iterable[_]] {})
    registerSerializer(iterableSerializer[Seq], new TypeToken[Seq[_]] {})
    registerSerializer(option, new TypeToken[Option[_]] {})
    registerSerializer(map, new TypeToken[Map[_, _]] {})
    registerSerializer(tuple2, new TypeToken[(_, _)] {})
    registerSerializer(tuple3, new TypeToken[(_, _, _)] {})
    registerSerializer(tuple4, new TypeToken[(_, _, _, _)] {})
    registerSerializer(tuple5, new TypeToken[(_, _, _, _, _)] {})
  }

  def iterableSerializer[Coll[A] <: Iterable[A]](implicit tag: ClassTag[Coll[_]],
                                                 cbf:          CanBuildFrom[Nothing, Any, Coll[_]]): TypeSerializer[Coll[_]] = new TypeSerializer[Coll[_]] {
    override def serialize(`type`: TypeToken[_], obj: Coll[_], node: ConfigurationNode): Unit = {
      val entryType   = `type`.resolveType(tag.runtimeClass.getTypeParameters.apply(0))
      val entrySerial = node.getOptions.getSerializers.get(entryType)
      if (entrySerial == null) throw new ObjectMappingException(s"No applicable type serializer for type $entryType when trying to serialize ${`type`}")

      node.setValue(ImmutableList.of)
      for (ent <- obj) {
        //Lot's of ugly casts
        entrySerial.asInstanceOf[TypeSerializer[Any]].serialize(entryType.asInstanceOf[TypeToken[Any]], ent, node.getAppendedNode)
      }
    }

    override def deserialize(`type`: TypeToken[_], node: ConfigurationNode): Coll[_] = {
      val entryType   = `type`.resolveType(tag.runtimeClass.getTypeParameters.apply(0))
      val entrySerial = node.getOptions.getSerializers.get(entryType)
      if (entrySerial == null) throw new ObjectMappingException(s"No applicable type serializer for type $entryType when trying to deserialize ${`type`}")

      if (node.hasListChildren) {
        val children = node.getChildrenList.asScala
        val builder  = cbf()
        builder.sizeHint(children)
        builder ++= children.map(n => entrySerial.deserialize(entryType, n))
        builder.result()
      } else {
        val unwrappedVal = node.getValue
        if (unwrappedVal != null) (cbf() += entrySerial.deserialize(entryType, node)).result()
        else cbf().result()
      }
    }
  }

  val map: TypeSerializer[Map[_, _]] = new TypeSerializer[Map[_, _]] {
    override def deserialize(`type`: TypeToken[_], node: ConfigurationNode): Map[_, _] =
      if (node.hasMapChildren) {
        val key         = `type`.resolveType(classOf[Map[_, _]].getTypeParameters.apply(0))
        val value       = `type`.resolveType(classOf[Map[_, _]].getTypeParameters.apply(1))
        val keySerial   = node.getOptions.getSerializers.get(key)
        val valueSerial = node.getOptions.getSerializers.get(value)
        if (keySerial == null) throw new ObjectMappingException(s"No type serializer available for type $key")
        if (valueSerial == null) throw new ObjectMappingException(s"No type serializer available for type $value")

        val ret = for (ent <- node.getChildrenMap.entrySet.asScala) yield {
          val keyValue   = keySerial.deserialize(key, SimpleConfigurationNode.root.setValue(ent.getKey))
          val valueValue = valueSerial.deserialize(value, ent.getValue)
          if (keyValue == null || valueValue == null) None
          else Some((keyValue, valueValue))
        }

        ret.flatten.toMap
      } else Map()
    override def serialize(`type`: TypeToken[_], obj: Map[_, _], node: ConfigurationNode): Unit = {
      val key         = `type`.resolveType(classOf[Map[_, _]].getTypeParameters.apply(0))
      val value       = `type`.resolveType(classOf[Map[_, _]].getTypeParameters.apply(1))
      val keySerial   = node.getOptions.getSerializers.get(key)
      val valueSerial = node.getOptions.getSerializers.get(value)
      if (keySerial == null) throw new ObjectMappingException(s"No type serializer available for type $key")
      if (valueSerial == null) throw new ObjectMappingException(s"No type serializer available for type $value")

      node.setValue(ImmutableMap.of)
      for ((k, v) <- obj) {
        val keyNode: SimpleConfigurationNode = SimpleConfigurationNode.root
        keySerial.asInstanceOf[TypeSerializer[Any]].serialize(key.asInstanceOf[TypeToken[Any]], k, keyNode)
        valueSerial
          .asInstanceOf[TypeSerializer[Any]]
          .serialize(value.asInstanceOf[TypeToken[Any]], v, node.getNode(keyNode.getValue())) //Don't remove the extra parenthesis on getValue, they make sure it compiles
      }
    }
  }

  val option: TypeSerializer[Option[_]] = new TypeSerializer[Option[_]] {
    override def deserialize(`type`: TypeToken[_], node: ConfigurationNode): Option[_] = {
      val entryType = `type`.resolveType(classOf[Option[_]].getTypeParameters.apply(0))

      Option(node.getValue(entryType))
    }
    override def serialize(`type`: TypeToken[_], obj: Option[_], node: ConfigurationNode): Unit = {
      val entryType = `type`.resolveType(classOf[Option[_]].getTypeParameters.apply(0))

      obj match {
        case Some(value) => node.setValue(entryType.asInstanceOf[TypeToken[Any]], value)
        case None        =>
      }
    }
  }

  private val classT2 = classOf[(_, _)]
  private val classT3 = classOf[(_, _, _)]
  private val classT4 = classOf[(_, _, _, _)]
  private val classT5 = classOf[(_, _, _, _, _)]

  val tuple2: TypeSerializer[(_, _)] = new TypeSerializer[(_, _)] {
    override def deserialize(`type`: TypeToken[_], node: ConfigurationNode): (_, _) = {
      val _1 = `type`.resolveType(classT2.getTypeParameters.apply(0))
      val _2 = `type`.resolveType(classT2.getTypeParameters.apply(1))

      val t1 = node.getNode("_1").getValue(_1)
      val t2 = node.getNode("_2").getValue(_2)
      (t1, t2)
    }

    override def serialize(`type`: TypeToken[_], obj: (_, _), node: ConfigurationNode): Unit = {
      val _1 = `type`.resolveType(classT2.getTypeParameters.apply(0))
      val _2 = `type`.resolveType(classT2.getTypeParameters.apply(1))

      node.getNode("_1").setValue(_1.asInstanceOf[TypeToken[Any]], obj._1)
      node.getNode("_2").setValue(_2.asInstanceOf[TypeToken[Any]], obj._2)
    }
  }

  val tuple3: TypeSerializer[(_, _, _)] = new TypeSerializer[(_, _, _)] {
    override def deserialize(`type`: TypeToken[_], node: ConfigurationNode): (_, _, _) = {
      val _1 = `type`.resolveType(classT3.getTypeParameters.apply(0))
      val _2 = `type`.resolveType(classT3.getTypeParameters.apply(1))
      val _3 = `type`.resolveType(classT3.getTypeParameters.apply(2))

      val t1 = node.getNode("_1").getValue(_1)
      val t2 = node.getNode("_2").getValue(_2)
      val t3 = node.getNode("_3").getValue(_3)
      (t1, t2, t3)
    }

    override def serialize(`type`: TypeToken[_], obj: (_, _, _), node: ConfigurationNode): Unit = {
      val _1 = `type`.resolveType(classT3.getTypeParameters.apply(0))
      val _2 = `type`.resolveType(classT3.getTypeParameters.apply(1))
      val _3 = `type`.resolveType(classT3.getTypeParameters.apply(2))

      node.getNode("_1").setValue(_1.asInstanceOf[TypeToken[Any]], obj._1)
      node.getNode("_2").setValue(_2.asInstanceOf[TypeToken[Any]], obj._2)
      node.getNode("_3").setValue(_3.asInstanceOf[TypeToken[Any]], obj._3)
    }
  }

  val tuple4: TypeSerializer[(_, _, _, _)] = new TypeSerializer[(_, _, _, _)] {
    override def deserialize(`type`: TypeToken[_], node: ConfigurationNode): (_, _, _, _) = {
      val _1 = `type`.resolveType(classT4.getTypeParameters.apply(0))
      val _2 = `type`.resolveType(classT4.getTypeParameters.apply(1))
      val _3 = `type`.resolveType(classT4.getTypeParameters.apply(2))
      val _4 = `type`.resolveType(classT4.getTypeParameters.apply(3))

      val t1 = node.getNode("_1").getValue(_1)
      val t2 = node.getNode("_2").getValue(_2)
      val t3 = node.getNode("_3").getValue(_3)
      val t4 = node.getNode("_4").getValue(_4)
      (t1, t2, t3, t4)
    }

    override def serialize(`type`: TypeToken[_], obj: (_, _, _, _), node: ConfigurationNode): Unit = {
      val _1 = `type`.resolveType(classT4.getTypeParameters.apply(0))
      val _2 = `type`.resolveType(classT4.getTypeParameters.apply(1))
      val _3 = `type`.resolveType(classT4.getTypeParameters.apply(2))
      val _4 = `type`.resolveType(classT4.getTypeParameters.apply(3))

      node.getNode("_1").setValue(_1.asInstanceOf[TypeToken[Any]], obj._1)
      node.getNode("_2").setValue(_2.asInstanceOf[TypeToken[Any]], obj._2)
      node.getNode("_3").setValue(_3.asInstanceOf[TypeToken[Any]], obj._3)
      node.getNode("_4").setValue(_4.asInstanceOf[TypeToken[Any]], obj._4)
    }
  }

  val tuple5: TypeSerializer[(_, _, _, _, _)] = new TypeSerializer[(_, _, _, _, _)] {
    override def deserialize(`type`: TypeToken[_], node: ConfigurationNode): (_, _, _, _, _) = {
      val _1 = `type`.resolveType(classT5.getTypeParameters.apply(0))
      val _2 = `type`.resolveType(classT5.getTypeParameters.apply(1))
      val _3 = `type`.resolveType(classT5.getTypeParameters.apply(2))
      val _4 = `type`.resolveType(classT5.getTypeParameters.apply(3))
      val _5 = `type`.resolveType(classT5.getTypeParameters.apply(4))

      val t1 = node.getNode("_1").getValue(_1)
      val t2 = node.getNode("_2").getValue(_2)
      val t3 = node.getNode("_3").getValue(_3)
      val t4 = node.getNode("_4").getValue(_4)
      val t5 = node.getNode("_5").getValue(_5)
      (t1, t2, t3, t4, t5)
    }

    override def serialize(`type`: TypeToken[_], obj: (_, _, _, _, _), node: ConfigurationNode): Unit = {
      val _1 = `type`.resolveType(classT5.getTypeParameters.apply(0))
      val _2 = `type`.resolveType(classT5.getTypeParameters.apply(1))
      val _3 = `type`.resolveType(classT5.getTypeParameters.apply(2))
      val _4 = `type`.resolveType(classT5.getTypeParameters.apply(3))
      val _5 = `type`.resolveType(classT5.getTypeParameters.apply(4))

      node.getNode("_1").setValue(_1.asInstanceOf[TypeToken[Any]], obj._1)
      node.getNode("_2").setValue(_2.asInstanceOf[TypeToken[Any]], obj._2)
      node.getNode("_3").setValue(_3.asInstanceOf[TypeToken[Any]], obj._3)
      node.getNode("_4").setValue(_4.asInstanceOf[TypeToken[Any]], obj._4)
      node.getNode("_5").setValue(_5.asInstanceOf[TypeToken[Any]], obj._5)
    }
  }

  //Too lazy to do the rest of all the tuples

}
