package team.ebi.aaa.attribute.impl

import com.google.common.reflect.TypeToken
import com.google.inject.{Inject, Singleton}
import io.izzel.amber.commons.i18n.AmberLocale
import team.ebi.aaa.api.Attribute
import team.ebi.aaa.api.context.{ContextualTransformer, InitializationContext, SummaryContext}
import team.ebi.aaa.api.data.visitor.impl.{AbstractMappingsVisitor, AbstractTemplatesVisitor, SimpleTemplatesVisitor}
import team.ebi.aaa.api.data.visitor.{MappingsVisitor, TemplatesVisitor}
import team.ebi.aaa.api.data.{Template, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.util._
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException

import scala.collection.mutable

@Singleton
class SuitAttribute @Inject()(manager: AttributeManager, locale: AmberLocale) extends Attribute[TemplateSlot.Equipment] { attr =>

  override def getDataClass: Class[TemplateSlot.Equipment] = classOf[TemplateSlot.Equipment]

  override def getDeserializationKey: String = "aaa-suit"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = slot match {
    case _: TemplateSlot.Equipment | _: TemplateSlot.Global => true
    case _ => false
  }

  override def initAttributes(node: ConfigurationNode): ContextualTransformer[InitializationContext, TemplatesVisitor] = {
    val strings = node.getList(TypeToken.of(classOf[String])).asScala
    val (valid, invalid) = (mutable.Buffer[TemplateSlot.Equipment](), mutable.Buffer[String]())
    for (slotTemplateString <- strings) Option(slotTemplateString).flatMap(Template.tryParse(_).asScala) match {
      case None => invalid += slotTemplateString
      case Some(template) => manager.slotMap.get(template) match {
        case Some(equipment: TemplateSlot.Equipment) => valid += equipment
        case _ => invalid += slotTemplateString
      }
    }
    if (invalid.nonEmpty) throw new ObjectMappingException(locale.getUnchecked("attribute.aaa-suit.failure", invalid.mkString("[", ", ", "]")).toPlain)
    new ContextualTransformer[InitializationContext, TemplatesVisitor] {
      override def transform(context: InitializationContext, parent: TemplatesVisitor): TemplatesVisitor = {
        val currentTemplate = context.getCurrentTemplate
        new SimpleTemplatesVisitor[TemplateSlot.Equipment](parent, currentTemplate, valid.asJava, attr) {
          override def visitTemplate(template: Template): MappingsVisitor = {
            val parent = super.visitTemplate(template)
            if (currentTemplate != template || !context.getSlot.isInstanceOf[TemplateSlot.Equipment]) parent else {
              parent.visitTemplates().visitTemplateEnd()
              parent.visitMappingsEnd()
              MappingsVisitor.EMPTY
            }
          }
        }
      }
    }
  }

  override def summarizeAttributes(): ContextualTransformer[SummaryContext, TemplatesVisitor] = {
    new ContextualTransformer[SummaryContext, TemplatesVisitor] {
      override def transform(context: SummaryContext, parent: TemplatesVisitor): TemplatesVisitor = {
        val availableTemplates: mutable.Map[Template, mutable.Set[Template]] = mutable.HashMap()
        var freezeEquipment = false

        class EquipmentVisitor(root: Template, parent: MappingsVisitor) extends AbstractMappingsVisitor(parent) {
          override def visitTemplates(): TemplatesVisitor = new AbstractTemplatesVisitor(super.visitTemplates()) {
            override def visitTemplate(template: Template): MappingsVisitor = {
              availableTemplates.getOrElseUpdate(root, mutable.Set()).add(template)
              new EquipmentVisitor(root, super.visitTemplate(template))
            }
          }
        }

        class GlobalVisitor(current: Option[Template], parent: MappingsVisitor) extends AbstractMappingsVisitor(parent) {
          type AttributeTuple[T] = (Attribute[T], T)

          private val tuples: mutable.Buffer[AttributeTuple[_]] = mutable.Buffer()

          private def visitTuple[T](e: AttributeTuple[T]): Unit = super.visitMapping(e._1, e._2)

          override def visitMapping[T](attribute: Attribute[T], data: T): Unit = tuples += attribute -> data

          override def visitTemplates(): TemplatesVisitor = {
            val suitSlotTemplates = tuples.filter(_._1 == attr).map(_._2).map(attr.getDataClass.cast).map(_.asTemplate)
            if (suitSlotTemplates.forall(availableTemplates.get(_).exists(set => current.exists(set.contains)))) {
              tuples.foreach(visitTuple(_))
              new AbstractTemplatesVisitor(super.visitTemplates()) {
                override def visitTemplate(template: Template): MappingsVisitor = {
                  new GlobalVisitor(Some(template), super.visitTemplate(template))
                }
              }
            } else {
              tuples.filter(_._1 == attr).foreach(visitTuple(_))
              super.visitTemplates().visitTemplateEnd()
              TemplatesVisitor.EMPTY
            }
          }

          override def visitMappingsEnd(): Unit = {
            super.visitMappingsEnd()
            freezeEquipment = true
          }
        }

        new AbstractTemplatesVisitor(parent) {
          private def wrongTemplateSlotOrderException(template: Template): Exception = {
            new IllegalStateException(s"Slot $template is visited after global slot")
          }

          override def visitTemplate(template: Template): MappingsVisitor = manager.slotMap.get(template) match {
            case Some(_: TemplateSlot.Equipment) if freezeEquipment => throw wrongTemplateSlotOrderException(template)
            case Some(_: TemplateSlot.Equipment) => new EquipmentVisitor(template, super.visitTemplate(template))
            case Some(_: TemplateSlot.Global) => new GlobalVisitor(None, super.visitTemplate(template))
            case _ => super.visitTemplate(template)
          }
        }
      }
    }
  }
}
