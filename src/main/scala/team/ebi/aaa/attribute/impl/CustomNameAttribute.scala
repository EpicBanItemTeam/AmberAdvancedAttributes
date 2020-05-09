package team.ebi.aaa.attribute.impl

import com.google.inject.{Inject, Singleton}
import org.slf4j.Logger
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa.api.Attribute
import team.ebi.aaa.api.data.{Mappings, MappingsRefreshEvent, TemplateSlot, TemplateSlots}
import team.ebi.aaa.data.CustomTemplates
import team.ebi.aaa.util._

@Singleton
class CustomNameAttribute @Inject()(implicit container: PluginContainer, logger: Logger) extends Attribute[String] {
  override def getDataClass: Class[String] = classOf[String]

  override def getDeserializationKey: String = "aaa-custom-name"

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
        val name = getMappingsIterator(event, slot).flatMap[String](_.getAttributeDataList(this).asScala).toSeq
        val succeed = item.get(classOf[CustomTemplates.Data]).isPresent && {
          val r = name.lastOption match {
            case None => item.remove(Keys.DISPLAY_NAME)
            case Some(head) => item.offer(Keys.DISPLAY_NAME, deserializeText(head, user))
          }
          r.isSuccessful && user.equip(slot.getEquipmentType, item)
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
