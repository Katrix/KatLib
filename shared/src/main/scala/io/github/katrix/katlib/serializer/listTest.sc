import java.io.{BufferedWriter, StringWriter}

import com.google.common.reflect.TypeToken

import io.github.katrix.katlib.serializer.TypeSerializerImpl._
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer
import shapeless.cachedImplicit

val jsonWriter = new StringWriter
val jsonLoader = HoconConfigurationLoader.builder
	.setSink(() => new BufferedWriter(jsonWriter))
	.build()
val root = jsonLoader.createEmptyNode()

val seqStringSerializer = cachedImplicit[TypeSerializer[Seq[String]]]
val seqStringToken = new TypeToken[Seq[String]] {}

seqStringSerializer.serialize(seqStringToken, Vector("1", "2", "3"), root)
root.getValue()
jsonLoader.save(root)
seqStringSerializer.deserialize(seqStringToken, root)
println(jsonWriter.getBuffer)