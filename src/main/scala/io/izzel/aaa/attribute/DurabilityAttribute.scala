package io.izzel.aaa.attribute

import io.izzel.aaa.api.Attribute
import io.izzel.aaa.api.data.TemplateSlot
import org.spongepowered.api.plugin.PluginContainer
import io.izzel.aaa.util.EventUtil._
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent

class DurabilityAttribute(implicit container: PluginContainer) extends Attribute[Int] {
  override def getDataClass: Class[Int] = classOf[Int]

  override def getDeserializationKey: String = "aaa-durability"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = slot.isInstanceOf[TemplateSlot.Equipment]

  listenTo[ChangeEntityEquipmentEvent] { event =>
    val _ = event.getTransaction // TODO
  }
}
