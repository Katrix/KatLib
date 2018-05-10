package net.katsstuff.katlib.scsponge.text

import java.util.UUID

import org.spongepowered.api.entity.{Entity, EntityType}
import org.spongepowered.api.item.inventory.ItemStackSnapshot
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.action.{HoverAction => SpongeHoverAction, TextActions}

object HoverAction {

  def showText(text: Text): SpongeHoverAction.ShowText = TextActions.showText(text)

  def showItem(item: ItemStackSnapshot): SpongeHoverAction.ShowItem = TextActions.showItem(item)

  //def showAchievement(achievement: Achievement) = TextActions.showAchievement(achievement)

  def showEntity(entity: SpongeHoverAction.ShowEntity.Ref): SpongeHoverAction.ShowEntity =
    TextActions.showEntity(entity)

  def showEntity(uuid: UUID, name: String, tpe: Option[EntityType]): SpongeHoverAction.ShowEntity =
    TextActions.showEntity(uuid, name, tpe.orNull)

  def showEntity(uuid: UUID, name: String): SpongeHoverAction.ShowEntity = TextActions.showEntity(uuid, name)

  def showEntity(entity: Entity, name: String): SpongeHoverAction.ShowEntity = TextActions.showEntity(entity, name)
}
