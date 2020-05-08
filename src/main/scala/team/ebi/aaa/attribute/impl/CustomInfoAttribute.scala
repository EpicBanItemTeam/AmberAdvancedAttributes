package team.ebi.aaa.attribute.impl

import com.google.inject.{Inject, Singleton}
import org.slf4j.Logger
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa.api.Attribute
import team.ebi.aaa.api.data.{Mappings, MappingsRefreshEvent, TemplateSlot}
import team.ebi.aaa.data.CustomTemplates
import team.ebi.aaa.util._

@Singleton
class CustomInfoAttribute @Inject()(implicit container: PluginContainer, logger: Logger) extends Attribute[String] {
  override def getDataClass: Class[String] = classOf[String]

  override def getDeserializationKey: String = "aaa-custom-info"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = slot.isInstanceOf[TemplateSlot.Equipment]

  listenTo[MappingsRefreshEvent] { event =>
    event.getAvailableSlots.asScala.foreach {
      case slot: TemplateSlot.Equipment => for (mappings <- event.getTargetMappings(slot).asScala) {
        val user = event.getTargetUser
        val item = user.getEquipped(slot.getEquipmentType).orElse(ItemStack.empty())
        val mappingsIterator = Mappings.flattenMappingsStream(mappings).iterator.asScala
        val (name, lore) = mappingsIterator.flatMap[(String, Seq[String])](m => +:.unapply(m.getAttributeDataList(this).asScala)).toSeq.unzip
        val succeed = item.get(classOf[CustomTemplates.Data]).isPresent && {
          val r1 = item.offer(Keys.ITEM_LORE, lore.flatten.map(deserializeText(_, user)).asJava)
          val r2 = name.lastOption match {
            case None => item.remove(Keys.DISPLAY_NAME)
            case Some(head) => item.offer(Keys.DISPLAY_NAME, deserializeText(head, user))
          }
          r1.isSuccessful && r2.isSuccessful && user.equip(slot.getEquipmentType, item)
        }
        if (!succeed) {
          val msg = s"Cannot apply custom info to ${slot.asTemplate} on ${user.getName}"
          logger.error(msg, new IllegalStateException(msg))
        }
      }
      case _ => ()
    }
  }
}
