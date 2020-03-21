package io.izzel.aaa.attribute

import java.util.concurrent.Callable
import java.util.function.Consumer

import com.google.inject.{Inject, Singleton}
import io.izzel.aaa.api.Attribute
import io.izzel.aaa.api.context.{InitializationContext, SummaryContext}
import io.izzel.aaa.api.data.visitor.impl.AbstractTemplatesVisitor
import io.izzel.aaa.api.data.visitor.{MappingsVisitor, TemplatesVisitor}
import io.izzel.aaa.api.data.{Mappings, Template, TemplateSlot, UnreachableSlotException}
import io.izzel.aaa.config.ConfigManager
import io.izzel.aaa.util._
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import org.slf4j.Logger
import org.spongepowered.api.entity.living.player.Player

import scala.collection.mutable

@Singleton
class MappingLoader @Inject()(logger: Logger, configManager: ConfigManager) {
  type Attributes = Map[String, Attribute[_]]

  type TemplateSlots = Map[Template, TemplateSlot]

  type BuilderGetter = TemplateSlot => Mappings.Builder

  type ConfigGetter = (Template, Attribute[_]) => Option[ConfigurationNode]

  class MappingsOpening(template: Template, mappings: Mappings) extends Consumer[TemplatesVisitor] {
    override def accept(t: TemplatesVisitor): Unit = {
      val visitor = t.visitTemplate(template)
      mappings.accept(visitor)
      t.visitTemplateEnd()
    }
  }

  class InitializationSerial(attributes: Attributes, configGetter: ConfigGetter, context: InitializationContext)
                            (parent: Consumer[TemplatesVisitor]) extends Consumer[TemplatesVisitor] {
    override def accept(t: TemplatesVisitor): Unit = {
      var newTemplateVisitor = t
      val attributesAvailable = attributes.values.filter {
        case a if a.isCompatibleWith(context.getSlot) => true
        case a => locally {
          val slotTemplate = context.getSlot.asTemplate
          logger.warn(s"Slot $slotTemplate is not compatible with ${a.getDeserializationKey} (it will be ignored)")
          false
        }
      }
      for (attribute <- attributesAvailable; node <- configGetter(context.getCurrentTemplate, attribute)) try {
        newTemplateVisitor = attribute.initAttributes(node).transform(context, newTemplateVisitor)
      } catch {
        case e: ObjectMappingException => locally {
          val template = context.getCurrentTemplate
          logger.error(s"Failed to initialize attribute ${attribute.getDeserializationKey} at template{$template}", e)
        }
      }
      parent.accept(newTemplateVisitor)
    }
  }

  class Multiplex(parents: Iterable[(Template, Consumer[TemplatesVisitor])]) extends Consumer[TemplatesVisitor] {
    override def accept(t: TemplatesVisitor): Unit = {
      for (entry <- parents) entry._2.accept(new AbstractTemplatesVisitor(TemplatesVisitor.EMPTY) {
        override def visitTemplate(template: Template): MappingsVisitor = {
          if (template == entry._1) t.visitTemplate(template) else {
            logger.error(s"Expected template{${entry._1}} but got template{$template}", new IllegalStateException)
            MappingsVisitor.EMPTY
          }
        }
      })
      t.visitTemplateEnd()
    }
  }

  class Combination(parents: Iterable[(TemplateSlot, Consumer[TemplatesVisitor])]) extends Consumer[TemplatesVisitor] {
    override def accept(t: TemplatesVisitor): Unit = {
      for ((slot, consumer) <- parents) {
        val visitor = t.visitTemplate(slot.asTemplate)
        consumer.accept(visitor.visitTemplates())
        visitor.visitMappingsEnd()
      }
      t.visitTemplateEnd()
    }
  }

  class SummarySerial(attributes: Attributes, context: SummaryContext)
                     (parent: Consumer[TemplatesVisitor]) extends Consumer[TemplatesVisitor] {
    override def accept(t: TemplatesVisitor): Unit = parent.accept(attributes.values.foldLeft(t) { (v, a) =>
      a.summarizeAttributes().transform(context, v)
    })
  }

  class BuildersEnding(slots: TemplateSlots, builderGetter: BuilderGetter)
                      (parent: Consumer[TemplatesVisitor]) extends Callable[Map[TemplateSlot, Mappings]] {
    private val map: mutable.Map[TemplateSlot, Mappings.Builder] = mutable.LinkedHashMap()

    override def call(): Map[TemplateSlot, Mappings] = {
      parent.accept(new AbstractTemplatesVisitor(TemplatesVisitor.EMPTY) {
        override def visitTemplate(template: Template): MappingsVisitor = slots.get(template) match {
          case Some(slot) => map.getOrElseUpdate(slot, builderGetter(slot))
          case None => locally {
            logger.error(s"Expected a template slot but got template{$template}", new IllegalStateException)
            MappingsVisitor.EMPTY
          }
        }
      })
      map.mapValues(_.build()).toMap
    }
  }

  class MappingsInitContext(player: Player, slot: TemplateSlot, template: Template) extends InitializationContext {
    override def getPlayer: Player = player

    override def getSlot: TemplateSlot = slot

    override def getCurrentTemplate: Template = template
  }

  class MappingsSummarizeContext(player: Player) extends SummaryContext {
    override def getPlayer: Player = player
  }

  private def getConfig(template: Template, attribute: Attribute[_]): Option[ConfigurationNode] = {
    configManager.get(template).map(_.getNode(attribute.getDeserializationKey)).filterNot(_.isVirtual)
  }

  def load(attributes: Attributes, slots: TemplateSlots)
          (player: Player): Map[TemplateSlot, Mappings] = {
    val summarySerial = loadGlobally(attributes, slots)(player)
    val buildersEnding = new BuildersEnding(slots, _ => Mappings.builder)(summarySerial)
    buildersEnding.call()
  }

  def loadPerSlot(attributes: Attributes, slot: TemplateSlot, templates: Iterable[Template])
                 (player: Player): Consumer[TemplatesVisitor] = {
    val multiplex = new Multiplex(templates.map { template =>
      val context = new MappingsInitContext(player, slot, template)
      val mappingsOpening = new MappingsOpening(template, Mappings.empty)
      val initializationSerial = new InitializationSerial(attributes, getConfig, context)(mappingsOpening)
      template -> initializationSerial
    })
    multiplex
  }

  def loadGlobally(attributes: Attributes, slots: TemplateSlots)
                  (player: Player): Consumer[TemplatesVisitor] = {
    val combination = new Combination(slots.values.flatMap(slot => try {
      val templates = slot.getTemplates(player).asScala.distinct // yeah, every template should be unique
      val multiplex = loadPerSlot(attributes, slot, templates)(player)
      Some(slot -> multiplex)
    } catch {
      case _: UnreachableSlotException => None
    }))
    val context = new MappingsSummarizeContext(player)
    val summarySerial = new SummarySerial(attributes, context)(combination)
    summarySerial
  }
}
