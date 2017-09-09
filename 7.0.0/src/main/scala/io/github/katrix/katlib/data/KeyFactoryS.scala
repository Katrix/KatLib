package io.github.katrix.katlib.data

import java.util.{Optional, List => JList, Map => JMap, Set => JSet}

import org.spongepowered.api.data.DataQuery
import org.spongepowered.api.data.key.{Key, KeyFactory}
import org.spongepowered.api.data.value.BaseValue
import org.spongepowered.api.data.value.mutable.{ListValue, MapValue, OptionalValue, SetValue}

import com.google.common.reflect.TypeToken

object KeyFactoryS {

  def makeSingleKey[A, B <: BaseValue[A]](query: DataQuery, id: String, name: String)(
      implicit typeToken: TypeToken[A],
      valueToken: TypeToken[B]
  ): Key[B] =
    KeyFactory.makeSingleKey(typeToken, valueToken, query, id, name)

  def makeListKey[A](query: DataQuery, id: String, name: String)(
      implicit listToken: TypeToken[JList[A]],
      valueToken: TypeToken[ListValue[A]]
  ): Key[ListValue[A]] =
    KeyFactory.makeListKey(listToken, valueToken, query, id, name)

  def makeSetKey[A](query: DataQuery, id: String, name: String)(
      implicit setToken: TypeToken[JSet[A]],
      valueToken: TypeToken[SetValue[A]]
  ): Key[SetValue[A]] =
    KeyFactory.makeSetKey(setToken, valueToken, query, id, name)

  def makeOptionalKey[A](query: DataQuery, id: String, name: String)(
      implicit optionalToken: TypeToken[Optional[A]],
      valueToken: TypeToken[OptionalValue[A]]
  ): Key[OptionalValue[A]] =
    KeyFactory.makeOptionalKey(optionalToken, valueToken, query, id, name)

  def makeMapKey[A, B](query: DataQuery, id: String, name: String)(
      implicit mapToken: TypeToken[JMap[A, B]],
      valueToken: TypeToken[MapValue[A, B]]
  ): Key[MapValue[A, B]] =
    KeyFactory.makeMapKey(mapToken, valueToken, query, id, name)

}
