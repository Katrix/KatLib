package io.github.katrix.katlib.persistant

import scala.collection.JavaConverters._

import com.google.common.collect.{ImmutableList, ImmutableMap}
import com.google.common.reflect.TypeToken

import ninja.leaping.configurate.objectmapping.ObjectMappingException
import ninja.leaping.configurate.objectmapping.serialize.{TypeSerializer, TypeSerializers}
import ninja.leaping.configurate.{ConfigurationNode, SimpleConfigurationNode}

object KatLibTypeSerializers {

  def registerScalaSerializers(): Unit = {
    val serializers = TypeSerializers.getDefaultSerializers
    serializers.registerType(new TypeToken[Iterable[_]]()     {}, iterable)
    serializers.registerType(new TypeToken[Map[_, _]]()       {}, map)
    serializers.registerType(new TypeToken[(_, _)]()          {}, tuple2)
    serializers.registerType(new TypeToken[(_, _, _)]()       {}, tuple3)
    serializers.registerType(new TypeToken[(_, _, _, _)]()    {}, tuple4)
    serializers.registerType(new TypeToken[(_, _, _, _, _)]() {}, tuple5)
  }

  //Mostly copy of the java list serializer
  val iterable: TypeSerializer[Iterable[_]] = new TypeSerializer[Iterable[_]] {
    override def deserialize(`type`: TypeToken[_], node: ConfigurationNode): Iterable[_] = {
      val entryType   = `type`.resolveType(classOf[Iterable[_]].getTypeParameters.apply(0))
      val entrySerial = node.getOptions.getSerializers.get(entryType)
      if (entrySerial == null) throw new ObjectMappingException(s"No applicable type serializer for type $entryType")

      if (node.hasListChildren) {
        val values = node.getChildrenList.asScala
        values.map(n => entrySerial.deserialize(entryType, n))
      } else {
        val unwrappedVal = node.getValue
        if (unwrappedVal != null) Seq(entrySerial.deserialize(entryType, node))
        else Nil
      }
    }

    override def serialize(`type`: TypeToken[_], obj: Iterable[_], node: ConfigurationNode): Unit = {
      val entryType   = `type`.resolveType(classOf[Iterable[_]].getTypeParameters.apply(0))
      val entrySerial = node.getOptions.getSerializers.get(entryType)
      if (entrySerial == null) throw new ObjectMappingException(s"No applicable type serializer for type $entryType")

      node.setValue(ImmutableList.of)
      for (ent <- obj) {
        //Lot's of ugly casts
        entrySerial.asInstanceOf[TypeSerializer[Any]].serialize(entryType.asInstanceOf[TypeToken[Any]], ent, node.getAppendedNode)
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

  val option = new TypeSerializer[Option[_]] {
    override def deserialize(`type`: TypeToken[_], node: ConfigurationNode): Option[_] = {
      val entryType = `type`.resolveType(classOf[Option[_]].getTypeParameters.apply(0))

      Option(node.getValue(entryType))
    }
    override def serialize(`type`: TypeToken[_], obj: Option[_], node: ConfigurationNode): Unit = {
      val entryType = `type`.resolveType(classOf[Option[_]].getTypeParameters.apply(0))

      obj match {
        case Some(value) => node.setValue(entryType, value)
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
