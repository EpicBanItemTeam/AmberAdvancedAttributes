package io.izzel.aaa.attribute.impl

import com.google.inject.{Inject, Singleton}
import io.izzel.aaa.api.Attribute
import io.izzel.aaa.api.data.TemplateSlot
import io.izzel.aaa.util._
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent
import org.spongepowered.api.plugin.PluginContainer

@Singleton
class DurabilityAttribute @Inject()(implicit container: PluginContainer) extends Attribute[Int] {
  override def getDataClass: Class[Int] = classOf[Int]

  override def getDeserializationKey: String = "aaa-durability"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = slot.isInstanceOf[TemplateSlot.Equipment]

  listenTo[ChangeEntityEquipmentEvent] { event =>
    val _ = event.getTransaction // TODO
  }
}
