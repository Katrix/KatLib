import java.io.{BufferedWriter, StringWriter}
import java.nio.file.Path

import scala.concurrent.ExecutionContextExecutorService

import org.slf4j.{Logger, LoggerFactory}
import org.spongepowered.api.scheduler.SpongeExecutorService

import com.google.common.reflect.TypeToken

import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.command.CmdPlugin
import io.github.katrix.katlib.helper.Implicits.PluginContainer
import io.github.katrix.katlib.persistant.Config
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer
import io.github.katrix.katlib.serializer.TypeSerializerImpl._
import ninja.leaping.configurate.gson.GsonConfigurationLoader
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import shapeless._

case class Foo(int: Int, string: String)
case class Bar(foo1: Foo, foo2: Foo, string: String)

implicit val plugin: KatPlugin = new KatPlugin {
	override def config: Config = ???
	override def syncExecutor: SpongeExecutorService = ???
	override def container: PluginContainer = ???
	override def logger: Logger = LoggerFactory.getLogger("test")
	override def syncExecutionContext: ExecutionContextExecutorService = ???
	override def configDir: Path = ???
	override def pluginCmd: CmdPlugin = ???
}

val listOptBarSerializer = cachedImplicit[TypeSerializer[List[Option[Bar]]]]
val listOptBarToken: TypeToken[List[Option[Bar]]] = new TypeToken[List[Option[Bar]]] {}

val toSerialize1 = Some(Bar(Foo(1, "t1"), Foo(2, "v16"), "yay"))
val toSerialize2 = Some(Bar(Foo(2, "t4"), Foo(4, "v9"), "boh"))
val toSerialize3 = None
val toSerialize = List(toSerialize1, toSerialize2, toSerialize3)

val hoconWriter = new StringWriter
val hoconLoader = HoconConfigurationLoader.builder
	.setSink(() => new BufferedWriter(hoconWriter))
	.build()
val root = hoconLoader.createEmptyNode()

listOptBarSerializer.serialize(listOptBarToken, toSerialize, root)
hoconLoader.save(root)

println(hoconWriter.getBuffer)
listOptBarSerializer.deserialize(listOptBarToken, root)

val jsonWriter = new StringWriter
val jsonLoader = GsonConfigurationLoader.builder
	.setSink(() => new BufferedWriter(jsonWriter))
	.build()
val root2 = jsonLoader.createEmptyNode()

val seqStringSerializer = cachedImplicit[TypeSerializer[Seq[String]]]
val seqStringToken = new TypeToken[Seq[String]] {}

seqStringSerializer.serialize(seqStringToken, Vector("1", "2", "3", "4", "5"), root2)
root2.getList(TypeToken.of(classOf[String]))
jsonLoader.save(root2)
println(jsonWriter.getBuffer)