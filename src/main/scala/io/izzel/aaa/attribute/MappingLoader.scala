package io.izzel.aaa.attribute

import java.util.concurrent.Callable
import java.util.function.Consumer

import com.google.gson.JsonPrimitive
import com.google.inject.{Inject, Singleton}
import io.izzel.aaa.api.Attribute
import io.izzel.aaa.api.context.{ContextualTransformer, InitializationContext, SummaryContext}
import io.izzel.aaa.api.data.visitor.impl.{AbstractMappingsVisitor, AbstractTemplatesVisitor}
import io.izzel.aaa.api.data.visitor.{MappingsVisitor, TemplatesVisitor}
import io.izzel.aaa.api.data.{Mappings, Template, TemplateSlot, UnreachableSlotDataException}
import io.izzel.aaa.config.ConfigManager
import io.izzel.aaa.util._
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import org.slf4j.Logger
import org.spongepowered.api.entity.living.player.Player

import scala.collection.mutable

@Singleton
class MappingLoader @Inject()(logger: Logger, configManager: ConfigManager) {
  type Attributes = Map[String, Attribute[_]]

  type TemplateSlots = Map[Template, TemplateSlot]

  type BuilderGetter = TemplateSlot => Mappings.Builder

  type Initializer = ContextualTransformer[InitializationContext, TemplatesVisitor]

  class MappingsOpening(template: Template, mappings: Mappings) extends Consumer[TemplatesVisitor] {
    override def accept(t: TemplatesVisitor): Unit = {
      val visitor = t.visitTemplate(template)
      mappings.accept(visitor)
      t.visitTemplateEnd()
    }
  }

  class InitializationSerial(attributes: Attributes, context: InitializationContext)
                            (parent: Consumer[TemplatesVisitor]) extends Consumer[TemplatesVisitor] {

    private val compatibilityError: mutable.Queue[(Attribute[_], Exception)] = mutable.Queue()
    private val initializationError: mutable.Queue[(Attribute[_], Exception)] = mutable.Queue()

    private def handleErrors(): Unit = {
      val slot = context.getSlot.asTemplate
      val template = context.getCurrentTemplate
      for ((attr, error) <- compatibilityError.dequeueAll(_ => true)) {
        logger.debug("Compatibility error", error)
        val attrDesc = s"${attr.getDeserializationKey} (loaded by template{$template})"
        logger.warn(s"Attribute $attrDesc is not compatible with slot $slot (it will be ignored)")
      }
      for ((attr, error) <- initializationError.dequeueAll(_ => true)) {
        logger.debug("Initialization error", error)
        val attrDesc = s"${attr.getDeserializationKey} (loaded by template{$template})"
        logger.error(s"Initialization of attribute $attrDesc failed (it will be ignored): ${error.getMessage}")
      }
    }

    private def getInitializer(template: Template, attribute: Attribute[_]): Option[Initializer] = {
      configManager.get(template).flatMap(_.get(attribute.getDeserializationKey))
    }

    override def accept(t: TemplatesVisitor): Unit = try {
      var newTemplateVisitor = t
      for (attr <- attributes.values; init <- getInitializer(context.getCurrentTemplate, attr)) try {
        if (!attr.isCompatibleWith(context.getSlot)) compatibilityError += attr -> new IllegalStateException() else {
          newTemplateVisitor = init.transform(context, newTemplateVisitor)
        }
      } catch {
        case e: ObjectMappingException => initializationError += attr -> new IllegalStateException(e)
      }
      parent.accept(newTemplateVisitor)
    } finally {
      handleErrors()
    }
  }

  class Multiplex(parents: Iterable[(Template, Consumer[TemplatesVisitor])]) extends Consumer[TemplatesVisitor] {
    override def accept(t: TemplatesVisitor): Unit = {
      for (entry <- parents) entry._2.accept(new AbstractTemplatesVisitor(TemplatesVisitor.EMPTY) {
        override def visitTemplate(template: Template): MappingsVisitor = {
          if (template == entry._1) t.visitTemplate(template) else {
            throw new IllegalStateException(s"Expected template{${entry._1}} but got template{$template}")
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

    private class LoggedVisitor(slot: TemplateSlot, parent: MappingsVisitor, index: Option[Int]) extends AbstractMappingsVisitor(parent) {
      if (index.isEmpty) {
        logger.debug(s"/* $slot * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * $slot */")
        logger.debug(s"/* $slot */ var visitor = Mappings.builder()")
      }

      private def escape(obj: Any): String = new JsonPrimitive(obj.toString).toString

      override def visitMapping[T](attribute: Attribute[T], data: T): Unit = {
        logger.debug(s"/* $slot */ visitor${index.mkString}.visitMapping(${escape(attribute.getDeserializationKey)}, ${escape(data)})")
        super.visitMapping(attribute, data)
      }

      override def visitTemplates(): TemplatesVisitor = {
        val newIndex = index.getOrElse(0) + 1
        logger.debug(s"/* $slot */ var visitor$newIndex = visitor${index.mkString}.visitTemplates()")
        new AbstractTemplatesVisitor(super.visitTemplates()) {
          override def visitTemplate(template: Template): MappingsVisitor = {
            val newerIndex = newIndex + 1
            logger.debug(s"/* $slot */ var visitor$newerIndex = visitor$newIndex.visitTemplate(${escape(template)})")
            new LoggedVisitor(slot, super.visitTemplate(template), Some(newerIndex))
          }

          override def visitTemplateEnd(): Unit = {
            logger.debug(s"/* $slot */ visitor$newIndex.visitTemplateEnd()")
            super.visitTemplateEnd()
          }
        }
      }

      override def visitMappingsEnd(): Unit = {
        logger.debug(s"/* $slot */ visitor${index.mkString}.visitMappingsEnd()")
        super.visitMappingsEnd()
        if (index.isEmpty) logger.debug(s"/* $slot * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * $slot */")
      }
    }

    override def call(): Map[TemplateSlot, Mappings] = {
      parent.accept(new AbstractTemplatesVisitor(TemplatesVisitor.EMPTY) {
        override def visitTemplate(template: Template): MappingsVisitor = slots.get(template) match {
          case Some(slot) => new LoggedVisitor(slot, map.getOrElseUpdate(slot, builderGetter(slot)), None)
          case None => throw new IllegalStateException(s"Expected a template slot but got template{$template}")
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
      val initializationSerial = new InitializationSerial(attributes, context)(mappingsOpening)
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
      case _: UnreachableSlotDataException => None
    }))
    val context = new MappingsSummarizeContext(player)
    val summarySerial = new SummarySerial(attributes, context)(combination)
    summarySerial
  }
}
