package team.ebi.aaa.attribute.impl

import java.util.function.BooleanSupplier

import com.google.common.reflect.TypeToken
import com.google.inject.{Inject, Singleton}
import team.ebi.aaa.api.Attribute
import team.ebi.aaa.api.context.{ContextualTransformer, InitializationContext}
import team.ebi.aaa.api.data.visitor.TemplatesVisitor
import team.ebi.aaa.api.data.visitor.impl.ConditionTemplatesVisitor
import team.ebi.aaa.api.data.{Template, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.util._
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
