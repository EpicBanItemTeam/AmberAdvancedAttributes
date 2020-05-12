package team.ebi.aaa.attribute.impl

import com.google.inject.{Inject, Singleton}
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.data.property.item.UseLimitProperty
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa
import team.ebi.aaa.api.Attribute
import team.ebi.aaa.api.data._
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.util._

@Singleton
class DurabilityAttribute @Inject()(manager: AttributeManager)(implicit container: PluginContainer) extends Attribute[Integer] {
  override def getDataClass: Class[Integer] = classOf[Integer]

  override def getDeserializationKey: String = "aaa-durability"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = slot.isInstanceOf[TemplateSlot.Equipment]

  private def recalculate(item: ItemStack, totalLimit: Int, durability: Int, limit: Int): Boolean = try {
    val extraLimit = totalLimit - limit
    val key = s"${aaa.name}ExtraDurability"
    val node = manager.getExtraData(item, key)
    val extraDurability = node.getInt(extraLimit)
    val cost = math.min(limit - durability, extraDurability)
    if (cost <= 0) false else {
      manager.setExtraData(item, key, node.setValue(math.min(extraLimit, extraDurability - cost)))
      item.offer[Integer](Keys.ITEM_DURABILITY, durability + cost)
      true
    }
  } catch {
    case _: UnreachableSlotDataException => false
  }

  private def handle(user: User, getMappings: TemplateSlot => Option[Mappings]): Unit = for {
    slot <- TemplateSlots.MAIN_HAND :: TemplateSlots.OFF_HAND :: Nil; mappings <- getMappings(slot)
    item = user.getEquipped(slot.getEquipmentType).orElse(ItemStack.empty)
    limitProperty <- item.getProperty(classOf[UseLimitProperty]).asScala
    durability <- item.get[Integer](Keys.ITEM_DURABILITY).asScala
  } {
    val limit = Int.unbox(limitProperty.getValue)
    val dataStream = Mappings.flattenDataStream(mappings, this)
    val totalLimit = (dataStream.iterator.asScala.map(Int.unbox) ++ (limit :: Nil)).max
    if (recalculate(item, totalLimit, Int.unbox(durability), limit)) {
      user.equip(slot.getEquipmentType, item)
    }
  }

  listenTo[MappingsRefreshEvent](event => handle(event.getTargetUser, event.getTargetMappings(_).asScala))
}
