package team.ebi.aaa.attribute.event

import org.spongepowered.api.Sponge
import org.spongepowered.api.event.Event
import org.spongepowered.api.event.cause.Cause
import team.ebi.aaa.api.data.{Template, TemplateSlot}

class TemplateSlotLoadEvent(on: (Template, TemplateSlot) => Unit) extends Event { // TODO: expose to api
  private val currentCause: Cause = Sponge.getCauseStackManager.getCurrentCause

  def register(slot: TemplateSlot): Unit = on(slot.asTemplate(), slot)

  override def getCause: Cause = currentCause
}
