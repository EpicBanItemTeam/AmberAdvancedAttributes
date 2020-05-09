package team.ebi.aaa.attribute.impl

import com.google.inject.{Inject, Singleton}
import io.izzel.amber.commons.i18n.AmberLocale
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import org.slf4j.Logger
import team.ebi.aaa
import team.ebi.aaa.api.Attribute
import team.ebi.aaa.api.context.{ContextualTransformer, InitializationContext}
import team.ebi.aaa.api.data.visitor.impl.{AbstractMappingsVisitor, AbstractTemplatesVisitor}
import team.ebi.aaa.api.data.visitor.{MappingsVisitor, TemplatesVisitor}
import team.ebi.aaa.api.data.{Template, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.attribute.mappings.MappingsGenerator
import team.ebi.aaa.util._

import scala.util.{DynamicVariable, Try}

@Singleton
class TemplateAttribute @Inject()(manager: AttributeManager, loader: MappingsGenerator, logger: Logger, locale: AmberLocale) extends Attribute[Nothing] {
  override def getDataClass: Class[Nothing] = classOf[Nothing]

  override def getDeserializationKey: String = aaa.templateKey

  override def isCompatibleWith(slot: TemplateSlot): Boolean = true

  override def initAttributes(node: ConfigurationNode): Transformer = new Transformer(node)

  private val templatePath: DynamicVariable[List[Template]] = new DynamicVariable(Nil)

  private def warnRecursive(template: Template): MappingsVisitor = {
    val pathString = templatePath.value.reverse.mkString(".")
    val msg = locale.getUnchecked("attribute.aaa-template.recursive", template, pathString).toPlain
    logger.debug(msg, new IllegalStateException)
    logger.warn(msg)
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
                  val msg = locale.getUnchecked("attribute.aaa-template.failure", childNode.getString).toPlain
                  throw new ObjectMappingException(msg)
                }
              }
              val summary = loader.generatePerSlot(manager.attributeMap, context.getSlot, templates)(context.getUser)
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
