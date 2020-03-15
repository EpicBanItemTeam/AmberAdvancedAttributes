package io.izzel.aaa.attribute

import com.google.inject.{Inject, Singleton}
import io.izzel.aaa
import io.izzel.aaa.api.Attribute
import io.izzel.aaa.api.context.{ContextualTransformer, InitializationContext}
import io.izzel.aaa.api.data.visitor.impl.{AbstractMappingsVisitor, AbstractTemplatesVisitor}
import io.izzel.aaa.api.data.visitor.{MappingsVisitor, TemplatesVisitor}
import io.izzel.aaa.api.data.{Template, TemplateSlot}
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import org.slf4j.Logger

import scala.collection.JavaConverters._
import scala.util.Try

@Singleton
class TemplateAttribute @Inject()(manager: AttributeManager, loader: MappingLoader, logger: Logger) extends Attribute[Nothing] {
  override def getDataClass: Class[Nothing] = classOf[Nothing]

  override def getDeserializationKey: String = aaa.templateKey

  override def isCompatibleWith(slot: TemplateSlot): Boolean = true

  override def initAttributes(node: ConfigurationNode): Transformer = new Transformer(node)

  class Transformer(node: ConfigurationNode) extends ContextualTransformer[InitializationContext, TemplatesVisitor] {
    override def transform(context: InitializationContext, parent: TemplatesVisitor): TemplatesVisitor = {
      new AbstractTemplatesVisitor(parent) {
        override def visitTemplate(template: Template): MappingsVisitor = {
          val parent = super.visitTemplate(template)
          if (context.getCurrentTemplate != template) parent else new TracedMappingsVisitor(parent, template :: Nil) {
            override def visitTemplates(): TemplatesVisitor = {
              val templates: Iterable[Template] = node.getChildrenList.asScala.map { childNode =>
                Try(Template.parse(childNode.getString)).getOrElse {
                  throw new ObjectMappingException(s"Invalid template name: ${childNode.getString}")
                }
              }
              val summary = loader.loadPerSlot(manager.attributeMap, context.getSlot, templates)(context.getPlayer)
              summary.accept(super.visitTemplates())
              TemplatesVisitor.EMPTY
            }
          }
        }
      }
    }
  }

  class TracedMappingsVisitor(parent: MappingsVisitor, path: List[Template]) extends AbstractMappingsVisitor(parent) {
    override def visitTemplates(): TemplatesVisitor = new TracedTemplatesVisitor(super.visitTemplates(), path)
  }

  class TracedTemplatesVisitor(parent: TemplatesVisitor, path: List[Template]) extends AbstractTemplatesVisitor(parent) {
    override def visitTemplate(template: Template): MappingsVisitor = template match {
      case _ if path.contains(template) => locally {
        logger.warn(s"Found that template{$template} is recursive and the recursive part will be dropped")
        MappingsVisitor.EMPTY
      }
      case _ => new TracedMappingsVisitor(super.visitTemplate(template), template :: path)
    }
  }
}
