package team.ebi.aaa.attribute.impl

import com.google.inject.{Inject, Singleton}
import org.spongepowered.api.data.`type`.HandTypes
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.data.property.item.UseLimitProperty
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

  listenTo[MappingsRefreshEvent] { event =>
    val user = event.getTargetUser
    event.getAvailableSlots.asScala.foreach {
      case TemplateSlots.MAIN_HAND => locally {
        val item = user.getItemInHand(HandTypes.MAIN_HAND).orElse(ItemStack.empty)
        for (durability <- item.get[Integer](Keys.ITEM_DURABILITY).asScala) {
          for (limit <- item.getProperty(classOf[UseLimitProperty]).asScala) {
            for (mappings <- event.getTargetMappings(TemplateSlots.MAIN_HAND).asScala) {
              val totalLimit = Mappings.flattenDataStream(mappings, this).iterator.asScala.map(Int.unbox).sum
              if (recalculate(item, totalLimit, Int.unbox(durability), Int.unbox(limit.getValue))) {
                user.setItemInHand(HandTypes.MAIN_HAND, item)
              }
            }
          }
        }
      }
      case TemplateSlots.OFF_HAND => locally {
        val item = user.getItemInHand(HandTypes.OFF_HAND).orElse(ItemStack.empty)
        for (durability <- item.get[Integer](Keys.ITEM_DURABILITY).asScala) {
          for (limit <- item.getProperty(classOf[UseLimitProperty]).asScala) {
            for (mappings <- event.getTargetMappings(TemplateSlots.OFF_HAND).asScala) {
              val totalLimit = Mappings.flattenDataStream(mappings, this).iterator.asScala.map(Int.unbox).sum
              if (recalculate(item, totalLimit, Int.unbox(durability), Int.unbox(limit.getValue))) {
                user.setItemInHand(HandTypes.OFF_HAND, item)
              }
            }
          }
        }
      }
      case _ => ()
    }
  }
}