package net.katsstuff.katlib.spongecommands

import io.leangen.geantyref.TypeToken
import net.katsstuff.katlib.spongecommands.opaques.ParameterValueBuilder
import net.katsstuff.katlib.helpers._
import net.katsstuff.minejson.text.Text
import net.kyori.adventure.text.Component
import org.spongepowered.api.ResourceKey
import org.spongepowered.api.block.BlockState
import org.spongepowered.api.command.parameter.{CommandContext, Parameter}
import org.spongepowered.api.data.persistence.DataContainer
import org.spongepowered.api.entity.Entity
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.entity.living.player.server.ServerPlayer
import org.spongepowered.api.item.inventory.ItemStackSnapshot
import org.spongepowered.api.registry.{RegistryHolder, RegistryType}
import org.spongepowered.api.util.Color
import org.spongepowered.api.world.server.ServerLocation
import org.spongepowered.api.world.server.storage.ServerWorldProperties
import org.spongepowered.math.vector.Vector3d
import org.spongepowered.plugin.PluginContainer

import java.net.{InetAddress, URL}
import java.time.{Duration, LocalDateTime}
import java.util.UUID
import scala.reflect.ClassTag
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

private[spongecommands] trait KatParameterList: 
  self: KatParameter.type =>
  type Id[A] = A

  def apply[A, F[_]](param: => ParameterValueBuilder[A], amount: Amount[F] = Amount.ExactlyOne): ChangableKatValueParameter[A, F] =
    import scala.reflect.Selectable.reflectiveSelectable
    val paramBuilder = amount match
      case Amount.ZeroOrOne  => () => param.optional()
      case Amount.OneOrMore  => () => param.consumeAllRemaining()
      case Amount.ZeroOrMore => () => param.optional().consumeAllRemaining()
      case Amount.ExactlyOne => () => param

    ComplexChangableKatValueParameter(paramBuilder, amount, identity)
  
  private val paramsClass = classOf[Parameter]
  
  inline def make[A](name: String): ChangableKatValueParameter[A, Id] =
    val method = paramsClass.getMethod(name)
    apply(method.invoke(null).asInstanceOf[ParameterValueBuilder[A]])

  val bigDecimal: ComplexChangableKatValueParameter[java.math.BigDecimal, BigDecimal, Id] = 
    make[java.math.BigDecimal]("bigDecimal").vmap(b => BigDecimal(b))
  
  val bigInt: ComplexChangableKatValueParameter[java.math.BigInteger, BigInt, Id] = 
    make[java.math.BigInteger]("bigInteger").vmap(b => BigInt(b))
  
  val blockState: ChangableKatValueParameter[BlockState, Id] = make("blockState")
  val boolean: ComplexChangableKatValueParameter[java.lang.Boolean, Boolean, Id] = make[java.lang.Boolean]("bool").vmap(b => b)
  val color: ChangableKatValueParameter[Color, Id] = make("color")
  val dataContainer: ChangableKatValueParameter[DataContainer, Id] = make("dataContainer")
  val dateTime: ChangableKatValueParameter[LocalDateTime, Id] = make("dateTime")
  val dateTimeOrNow: ChangableKatValueParameter[LocalDateTime, Id] = make("dateTimeOrNow")
  val javaDuration: ChangableKatValueParameter[Duration, Id] = make("duration")
  val double: ComplexChangableKatValueParameter[java.lang.Double, Double, Id] = make[java.lang.Double]("doubleNumber").vmap(v => v)
  val entity: ChangableKatValueParameter[Entity, Id] = make("entity")
  val entityOrSource: ChangableKatValueParameter[Entity, Id] = make("entityOrSource")
  val entityOrTarget: ChangableKatValueParameter[Entity, Id] = make("entityOrTarget")
  val formattingCodeText: ComplexChangableKatValueParameter[Component, Text, Id] = make[Component]("formattingCodeText").vmap(_.toKatText)
  val formattingCodeTextOfRemainingElements: ComplexChangableKatValueParameter[Component, Text, Id] = make[Component]("formattingCodeTextOfRemainingElements").vmap(_.toKatText)
  val int: ComplexChangableKatValueParameter[java.lang.Integer, Int, Id] = make[java.lang.Integer]("integerNumber").vmap(i => i)
  val ip: ChangableKatValueParameter[InetAddress, Id] = make("ip")
  val ipOrSource: ChangableKatValueParameter[InetAddress, Id] = make("ipOrSource")
  val itemStackSnapshot: ChangableKatValueParameter[ItemStackSnapshot, Id] = make("itemStackSnapshot")
  val jsonText: ComplexChangableKatValueParameter[Component, Text, Id] = make[Component]("jsonText").vmap(_.toKatText)
  val jsonTextOfRemainingElements: ComplexChangableKatValueParameter[Component, Text, Id] = make[Component]("jsonTextOfRemainingElements").vmap(_.toKatText)
  val location: ChangableKatValueParameter[ServerLocation, Id] = make("location")
  val locationOnlineOnly: ChangableKatValueParameter[ServerLocation, Id] = make("locationOnlineOnly")
  val long: ComplexChangableKatValueParameter[java.lang.Long, Long, Id] = make[java.lang.Long]("longNumber").vmap(l => l)
  val player: ChangableKatValueParameter[ServerPlayer, Id] = make("player")
  val playerOrSource: ChangableKatValueParameter[ServerPlayer, Id] = make("playerOrSource")
  val plugin: ChangableKatValueParameter[PluginContainer, Id] = make("plugin")
  def rangedDouble(min: Double, max: Double): ChangableKatValueParameter[Double, Id] = ???
  def rangedInt(min: Int, max: Int): ChangableKatValueParameter[Int, Id] = ???
  def rangedInt(range: Range): ChangableKatValueParameter[Int, Id] = ???
  val remainingJoinedString: ChangableKatValueParameter[String, Id] = make("remainingJoinedString")
  val resourceKey: ChangableKatValueParameter[ResourceKey, Id] = make("resourceKey")
  val string: ChangableKatValueParameter[String, Id] = make("string")
  val url: ChangableKatValueParameter[URL, Id] = make("url")
  val user: ChangableKatValueParameter[User, Id] = make("user")
  val userOrSource: ChangableKatValueParameter[User, Id] = make("userOrSource")
  val uuid: ChangableKatValueParameter[UUID, Id] = make("uuid")
  val vector3d: ChangableKatValueParameter[Vector3d, Id] = make("vector3d")
  val worldProperties: ChangableKatValueParameter[ServerWorldProperties, Id] = make("worldProperties")
  val worldPropertiesOnlineOnly: ChangableKatValueParameter[ServerWorldProperties, Id] = make("worldPropertiesOnlineOnly")

  def registryElement[A: TypeToken](
    holder: CommandContext => RegistryHolder,
    key: RegistryType[A],
    defaultNamespaces: String*
  ): ChangableKatValueParameter[A, Id] = ???

  def registryElement[A: TypeToken](
    key: RegistryType[A],
    defaultNamespaces: String*
  ): ChangableKatValueParameter[A, Id] = ???

  def choices(choices: String*): ChangableKatValueParameter[String, Id] = ???
  def choices[A: TypeToken](choices: Map[String, A]): ChangableKatValueParameter[A, Id] = ???
  def choices[A: TypeToken](getValue: String => A, choices: () => Iterable[String]): ChangableKatValueParameter[A, Id] = ???

  def enumValue[A <: Enum[A]: ClassTag]: ChangableKatValueParameter[A, Id] = ???

  def literal[A: TypeToken](value: A, literal: String*): ChangableKatValueParameter[A, Id] = ???
  def literal[A: TypeToken](value: A, literals: () => Iterable[String]): ChangableKatValueParameter[A, Id] = ???

  def oneOfEither[P1 <: Tuple, P2 <: Tuple](first: KatParameters[P1], second: KatParameters[P2]): KatParameter[Either[P1, P2]] = ???
  def oneOfEither[A, B](first: KatParameter[A], second: KatParameter[B]): KatParameter[Either[A, B]] = ???

  def oneOf[A, B](first: KatParameter[A], second: KatParameter[B]): KatParameter[A | B] = ???
  def oneOf[T <: Tuple](values: Tuple.Map[T, KatParameter]): KatParameter[Tuple.Union[T]] = ???
end KatParameterList
