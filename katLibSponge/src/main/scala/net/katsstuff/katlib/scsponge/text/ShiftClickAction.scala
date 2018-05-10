package net.katsstuff.katlib.scsponge.text

import org.spongepowered.api.text.action.{ShiftClickAction => SpongeShiftClickAction, TextActions}

object ShiftClickAction {

  def insertText(text: String): SpongeShiftClickAction.InsertText = TextActions.insertText(text)
}
