package team.ebi.aaa.attribute.event

import java.util.Optional

import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.event.cause.Cause
import team.ebi.aaa.api.data.{Mappings, MappingsRefreshEvent, TemplateSlot}
import team.ebi.aaa.util._

class CacheRefreshEvent(targetUser: User, value: CacheRefreshValue) extends MappingsRefreshEvent {
  private val currentCause: Cause = Sponge.getCauseStackManager.pushCause(targetUser).pushCause(value).getCurrentCause

  override def getTargetMappings(slot: TemplateSlot): Optional[Mappings] = value.get(slot).asJava

  override def getAvailableSlots: java.util.Set[_ <: TemplateSlot] = value.keys.asJava

  override def getTargetUser: User = targetUser

  override def getCause: Cause = currentCause
}
