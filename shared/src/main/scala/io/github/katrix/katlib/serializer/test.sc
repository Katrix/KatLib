import java.io.{BufferedWriter, StringWriter}

import com.google.common.reflect.TypeToken

import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer
import shapeless._
import io.github.katrix.katlib.serializer.TypeSerializerImpl._

case class Foo(int: Int, string: String)
case class Bar(foo1: Foo, foo2: Foo, string: String)

val listOptBarSerializer = cachedImplicit[TypeSerializer[List[Option[Bar]]]]
val listOptBarToken: TypeToken[List[Option[Bar]]] = new TypeToken[List[Option[Bar]]] {}

val toSerialize1 = Some(Bar(Foo(1, "t1"), Foo(2, "v16"), "yay"))
val toSerialize2 = Some(Bar(Foo(2, "t4"), Foo(4, "v9"), "boh"))
val toSerialize3 = None
val toSerialize = List(toSerialize1, toSerialize2, toSerialize3, toSerialize2)

val hoconWriter = new StringWriter
val hoconLoader = HoconConfigurationLoader.builder
	.setSink(() => new BufferedWriter(hoconWriter))
	.build()
val root = hoconLoader.createEmptyNode()

listOptBarSerializer.serialize(listOptBarToken, toSerialize, root)
hoconLoader.save(root)

println(hoconWriter.getBuffer)
listOptBarSerializer.deserialize(listOptBarToken, root)