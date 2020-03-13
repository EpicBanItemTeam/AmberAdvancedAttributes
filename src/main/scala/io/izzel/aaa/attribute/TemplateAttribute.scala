package io.izzel.aaa.attribute

import io.izzel.aaa.api.Attribute
import io.izzel.aaa.api.context.{ContextualTransformer, InitializationContext}
import io.izzel.aaa.api.data.visitor.impl.{AbstractMappingsVisitor, AbstractTemplatesVisitor}
import io.izzel.aaa.api.data.visitor.{MappingsVisitor, TemplatesVisitor}
import io.izzel.aaa.api.data.{Template, TemplateSlot}
import io.izzel.aaa
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

class TemplateAttribute extends Attribute[Nothing] {
  override def getDataClass: Class[Nothing] = classOf[Nothing]

  override def getDeserializationKey: String = aaa.templateKey

  override def isCompatibleWith(slot: TemplateSlot): Boolean = true

  override def initAttributes(node: ConfigurationNode): Transformer = new Transformer(node)

  class Transformer(node: ConfigurationNode) extends ContextualTransformer[InitializationContext, TemplatesVisitor] {
    override def transform(context: InitializationContext, parent: TemplatesVisitor): TemplatesVisitor = {
      new AbstractTemplatesVisitor(parent) {
        override def visitTemplate(template: Template): MappingsVisitor = {
          val parent = super.visitTemplate(template)
          if (context.getCurrentTemplate != template) parent else new AbstractMappingsVisitor(parent) {
            override def visitTemplates(): TemplatesVisitor = {
              val parent = super.visitTemplates()
              for (childNode <- node.getChildrenList.asScala) parent.visitTemplate(try {
                Template.parse(childNode.getString(""))
              } catch {
                case NonFatal(e) => throw new ObjectMappingException(e);
              })
              parent
            }
          }
        }
      }
    }
  }
}
