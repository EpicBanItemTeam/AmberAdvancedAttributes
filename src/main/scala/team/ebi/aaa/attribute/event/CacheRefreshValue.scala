package team.ebi.aaa.attribute.event

import team.ebi.aaa.api.data.{Mappings, TemplateSlot}
import team.ebi.aaa.util._

class CacheRefreshValue(value: Map[TemplateSlot, Mappings]) {
  def asJava: java.util.Map[TemplateSlot, Mappings] = value.asJava

  def get(slot: TemplateSlot): Option[Mappings] = value.get(slot)

  def keys: Set[TemplateSlot] = value.keySet
}
