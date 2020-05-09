package team.ebi.aaa.attribute.impl

import com.google.inject.{Inject, Singleton}
import io.izzel.amber.commons.i18n.AmberLocale
import org.slf4j.Logger
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa.api.Attribute
import team.ebi.aaa.api.data.{Mappings, MappingsRefreshEvent, TemplateSlot, TemplateSlots}
import team.ebi.aaa.data.CustomTemplates
import team.ebi.aaa.util._

@Singleton
class CustomInfoAttribute @Inject()(implicit container: PluginContainer, logger: Logger, locale: AmberLocale) extends Attribute[String] {
  override def getDataClass: Class[String] = classOf[String]

  override def getDeserializationKey: String = "aaa-custom-info"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = true

  // noinspection DuplicatedCode
  private def getMappingsIterator(event: MappingsRefreshEvent, slot: TemplateSlot.Equipment): Iterator[Mappings] = {
    val forEmpty = java.util.stream.Stream.empty
    val forSlot = event.getTargetMappings(slot).asScala.map(Mappings.flattenMappingsStream)
    val forGlobal = event.getTargetMappings(TemplateSlots.GLOBAL).asScala.map(Mappings.flattenMappingsStream)
    java.util.stream.Stream.concat(forSlot.getOrElse(forEmpty), forGlobal.getOrElse(forEmpty)).iterator.asScala
  }

  // noinspection DuplicatedCode
  listenTo[MappingsRefreshEvent] { event =>
    event.getAvailableSlots.asScala.foreach {
      case slot: TemplateSlot.Equipment => locally {
        val user = event.getTargetUser
        val item = user.getEquipped(slot.getEquipmentType).orElse(ItemStack.empty)
        val lore = getMappingsIterator(event, slot).flatMap[String](_.getAttributeDataList(this).asScala).toSeq
        val succeed = item.get(classOf[CustomTemplates.Data]).isPresent && {
          val r = item.offer(Keys.ITEM_LORE, lore.map(deserializeText(_, user)).asJava)
          r.isSuccessful && user.equip(slot.getEquipmentType, item)
        }
        if (!succeed) {
          val msg = locale.getUnchecked("attribute.aaa-custom-info.failure", slot.asTemplate, user.getName).toPlain
          logger.error(msg, new IllegalStateException(msg))
        }
      }
      case _ => ()
    }
  }
}
