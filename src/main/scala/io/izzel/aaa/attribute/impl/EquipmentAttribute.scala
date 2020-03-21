package io.izzel.aaa.attribute.impl

import java.util.function.BooleanSupplier

import com.google.common.reflect.TypeToken
import com.google.inject.{Inject, Singleton}
import io.izzel.aaa.api.Attribute
import io.izzel.aaa.api.context.{ContextualTransformer, InitializationContext}
import io.izzel.aaa.api.data.visitor.TemplatesVisitor
import io.izzel.aaa.api.data.visitor.impl.ConditionTemplatesVisitor
import io.izzel.aaa.api.data.{Template, TemplateSlot}
import io.izzel.aaa.attribute.AttributeManager
import io.izzel.aaa.util._
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException

@Singleton
class EquipmentAttribute @Inject()(manager: AttributeManager) extends Attribute[Nothing] {
  override def getDataClass: Class[Nothing] = classOf[Nothing]

  override def getDeserializationKey: String = "aaa-equipment"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = slot.isInstanceOf[TemplateSlot.Equipment]

  override def initAttributes(node: ConfigurationNode): ContextualTransformer[InitializationContext, TemplatesVisitor] = {
    val strings = node.getList(TypeToken.of(classOf[String])).asScala
    val invalid = strings.filterNot(Option(_).flatMap(Template.tryParse(_).asScala).exists(manager.slotMap.contains))
    if (invalid.nonEmpty) throw new ObjectMappingException(s"Invalid string(s): ${invalid.mkString("[", ", ", "]")}")
    new ContextualTransformer[InitializationContext, TemplatesVisitor] {
      override def transform(context: InitializationContext, parent: TemplatesVisitor): TemplatesVisitor = {
        new ConditionTemplatesVisitor(parent, context.getCurrentTemplate, new BooleanSupplier {
          override def getAsBoolean: Boolean = strings.contains(context.getSlot.asTemplate.toString)
        })
      }
    }
  }
}
