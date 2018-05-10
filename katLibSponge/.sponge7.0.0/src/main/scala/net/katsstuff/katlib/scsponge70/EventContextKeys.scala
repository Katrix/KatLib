package net.katsstuff.katlib.scsponge70

import org.spongepowered.api.block.BlockSnapshot
import org.spongepowered.api.entity.Entity
import org.spongepowered.api.entity.living.Living
import org.spongepowered.api.entity.living.player.{Player, User}
import org.spongepowered.api.entity.projectile.source.ProjectileSource
import org.spongepowered.api.event.cause.entity.damage.DamageType
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource
import org.spongepowered.api.event.cause.entity.dismount.DismountType
import org.spongepowered.api.event.cause.entity.spawn.SpawnType
import org.spongepowered.api.event.cause.entity.teleport.TeleportType
import org.spongepowered.api.item.inventory.ItemStackSnapshot
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.profile.GameProfile
import org.spongepowered.api.service.ServiceManager
import org.spongepowered.api.world.World

import org.spongepowered.api.event.cause.{EventContextKeys => SpongeKeys}

object EventContextKeys {

  val BlockHit: EventContextKey[BlockSnapshot] = SpongeKeys.BLOCK_HIT

  val Creator: EventContextKey[User] = SpongeKeys.CREATOR

  val DamageType: EventContextKey[DamageType] = SpongeKeys.DAMAGE_TYPE

  val DismountType: EventContextKey[DismountType] = SpongeKeys.DISMOUNT_TYPE

  val EntityHit: EventContextKey[Entity] = SpongeKeys.ENTITY_HIT

  val FakePlayer: EventContextKey[Player] = SpongeKeys.FAKE_PLAYER

  val FireSpread: EventContextKey[World] = SpongeKeys.FIRE_SPREAD

  val Igniter: EventContextKey[Living] = SpongeKeys.IGNITER

  val LastDamageSource: EventContextKey[DamageSource] = SpongeKeys.LAST_DAMAGE_SOURCE

  val LeavesDecay: EventContextKey[World] = SpongeKeys.LEAVES_DECAY

  val LiquidMix: EventContextKey[World] = SpongeKeys.LIQUID_MIX

  val Notifier: EventContextKey[User] = SpongeKeys.NOTIFIER

  val Owner: EventContextKey[User] = SpongeKeys.OWNER

  val PistonExtend: EventContextKey[World] = SpongeKeys.PISTON_EXTEND

  val PistonRetract: EventContextKey[World] = SpongeKeys.PISTON_RETRACT

  val Player: EventContextKey[Player] = SpongeKeys.PLAYER

  val PlayerBreak: EventContextKey[World] = SpongeKeys.PLAYER_BREAK

  val PlayerPlace: EventContextKey[World] = SpongeKeys.PLAYER_PLACE

  val PlayerSimulated: EventContextKey[GameProfile] = SpongeKeys.PLAYER_SIMULATED

  val Plugin: EventContextKey[PluginContainer] = SpongeKeys.PLUGIN

  val ProjectileSource: EventContextKey[ProjectileSource] = SpongeKeys.PROJECTILE_SOURCE

  val ServiceManager: EventContextKey[ServiceManager] = SpongeKeys.SERVICE_MANAGER

  val SpawnType: EventContextKey[SpawnType] = SpongeKeys.SPAWN_TYPE

  val TeleportType: EventContextKey[TeleportType] = SpongeKeys.TELEPORT_TYPE

  val Thrower: EventContextKey[ProjectileSource] = SpongeKeys.THROWER

  val UsedItem: EventContextKey[ItemStackSnapshot] = SpongeKeys.USED_ITEM

  val Weapon: EventContextKey[ItemStackSnapshot] = SpongeKeys.WEAPON

}
