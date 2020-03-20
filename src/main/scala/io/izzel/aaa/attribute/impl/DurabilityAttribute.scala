package io.izzel.aaa.attribute.impl

import com.google.inject.{Inject, Singleton}
import io.izzel.aaa.api.Attribute
import io.izzel.aaa.api.data.TemplateSlot
import io.izzel.aaa.util._
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent
import org.spongepowered.api.plugin.PluginContainer

@Singleton
class DurabilityAttribute @Inject()(implicit container: PluginContainer) extends Attribute[Integer] {
  override def getDataClass: Class[Integer] = classOf[Integer]

  override def getDeserializationKey: String = "aaa-durability"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = slot.isInstanceOf[TemplateSlot.Equipment]

  listenTo[ChangeEntityEquipmentEvent] { event =>
    val _ = event.getTransaction // TODO
  }
}
