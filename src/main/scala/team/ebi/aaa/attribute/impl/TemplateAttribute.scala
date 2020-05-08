package team.ebi.aaa.attribute.impl

import com.google.inject.{Inject, Singleton}
import team.ebi.aaa
import team.ebi.aaa.api.Attribute
import team.ebi.aaa.api.context.{ContextualTransformer, InitializationContext}
import team.ebi.aaa.api.data.visitor.impl.{AbstractMappingsVisitor, AbstractTemplatesVisitor}
import team.ebi.aaa.api.data.visitor.{MappingsVisitor, TemplatesVisitor}
import team.ebi.aaa.api.data.{Template, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.util._
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import org.slf4j.Logger
import team.ebi.aaa.attribute.mappings.MappingsGenerator

import scala.util.{DynamicVariable, Try}

@Singleton
class TemplateAttribute @Inject()(manager: AttributeManager, loader: MappingsGenerator, logger: Logger) extends Attribute[Nothing] {
  override def getDataClass: Class[Nothing] = classOf[Nothing]

  override def getDeserializationKey: String = aaa.templateKey

  override def isCompatibleWith(slot: TemplateSlot): Boolean = true

  override def initAttributes(node: ConfigurationNode): Transformer = new Transformer(node)

  private val templatePath: DynamicVariable[List[Template]] = new DynamicVariable(Nil)

  private def warnRecursive(template: Template): MappingsVisitor = {
    logger.debug("", new IllegalStateException)
    val pathString = templatePath.value.reverse.mkString(".")
    logger.warn(s"Found recursive part of template{$template} (in $pathString) and the recursive part will be dropped")
    MappingsVisitor.EMPTY
  }

  class Transformer(node: ConfigurationNode) extends ContextualTransformer[InitializationContext, TemplatesVisitor] {
    override def transform(context: InitializationContext, parent: TemplatesVisitor): TemplatesVisitor = {
      new AbstractTemplatesVisitor(parent) {
        override def visitTemplate(template: Template): MappingsVisitor = template match {
          case _ if context.getCurrentTemplate != template => super.visitTemplate(template)
          case _ if templatePath.value.contains(template) => warnRecursive(template)
          case _ => new AbstractMappingsVisitor(super.visitTemplate(template)) {
            override def visitTemplates(): TemplatesVisitor = {
              val templates: Iterable[Template] = node.getChildrenList.asScala.map { childNode =>
                Try(Template.parse(childNode.getString)).getOrElse {
                  throw new ObjectMappingException(s"Invalid template name: ${childNode.getString}")
                }
              }
              val summary = loader.generatePerSlot(manager.attributeMap, context.getSlot, templates)(context.getPlayer)
              templatePath.withValue(template :: templatePath.value) {
                Option(super.visitTemplates()).filterNot(_ == TemplatesVisitor.EMPTY).foreach(summary.accept)
              }
              TemplatesVisitor.EMPTY
            }
          }
        }
      }
    }
  }
}
