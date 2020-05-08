package team.ebi.aaa.attribute.impl

import com.google.inject.{Inject, Singleton}
import team.ebi.aaa.api.Attribute
import team.ebi.aaa.api.data.{Mappings, MappingsRefreshEvent, TemplateSlot}
import team.ebi.aaa.data.CustomTemplates
import team.ebi.aaa.util._
import org.slf4j.Logger
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.text.serializer.TextSerializers

@Singleton
class CustomInfoAttribute @Inject()(implicit container: PluginContainer, logger: Logger) extends Attribute[String] {
  override def getDataClass: Class[String] = classOf[String]

  override def getDeserializationKey: String = "aaa-custom-info"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = slot.isInstanceOf[TemplateSlot.Equipment]

  listenTo[MappingsRefreshEvent] { event =>
    event.getAvailableSlots.asScala.foreach {
      case slot: TemplateSlot.Equipment => for (mappings <- event.getTargetMappings(slot).asScala) {
        val player = event.getTargetEntity
        val succeed = Mappings.dataStream(mappings, this, true).iterator.asScala.toSeq match {
          case Nil => locally {
            val item = player.getEquipped(slot.getEquipmentType).orElse(ItemStack.empty())
            item.get(classOf[CustomTemplates.Data]).isPresent && {
              val r1 = item.remove(Keys.ITEM_LORE)
              val r2 = item.remove(Keys.DISPLAY_NAME)
              r1.isSuccessful && r2.isSuccessful && player.equip(slot.getEquipmentType, item)
            }
          }
          case name +: lore => locally {
            val item = player.getEquipped(slot.getEquipmentType).orElse(ItemStack.empty())
            item.get(classOf[CustomTemplates.Data]).isPresent && {
              val r1 = item.offer(Keys.ITEM_LORE, lore.map(TextSerializers.FORMATTING_CODE.deserialize).asJava)
              val r2 = item.offer(Keys.DISPLAY_NAME, TextSerializers.FORMATTING_CODE.deserialize(name))
              r1.isSuccessful && r2.isSuccessful && player.equip(slot.getEquipmentType, item)
            }
          }
        }
        if (!succeed) {
          val msg = s"Cannot apply custom info to ${slot.asTemplate} on ${player.getName}"
          logger.error(msg, new IllegalStateException(msg))
        }
      }
      case _ => ()
    }
  }
}
