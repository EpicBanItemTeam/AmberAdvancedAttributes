package io.izzel.aaa.attribute

import com.google.common.collect.Lists
import com.google.inject.{Inject, Singleton}
import io.izzel.aaa.api.{Attribute, AttributeService}
import io.izzel.aaa.util.EventUtil._
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.event.game.state.{GamePostInitializationEvent, GameStartingServerEvent}
import org.spongepowered.api.plugin.PluginContainer

import scala.collection.JavaConverters._
import scala.util.continuations.reset

@Singleton
class AttributeManager @Inject()(implicit container: PluginContainer, logger: Logger) extends AttributeService {
  private var attributes: List[Attribute[_]] = Nil

  private object AttributeLoadEvent extends Attribute.LoadEvent {
    // reverse the list since add(0, list) is called quite frequently so that it will be added as last element actually
    private[AttributeManager] val list: java.util.List[Attribute[_]] = Lists.reverse(new java.util.ArrayList)

    private val spongeCurrentCause: Cause = Sponge.getCauseStackManager.getCurrentCause

    override def getAttributesToBeRegistered: java.util.List[Attribute[_]] = list

    override def getCause: Cause = spongeCurrentCause
  }

  reset {
    waitFor[GamePostInitializationEvent]
    logger.info("Injecting service ...")
    Sponge.getServiceManager.setProvider(container, classOf[AttributeService], this)

    waitFor[GameStartingServerEvent]
    logger.info("Registering attributes ...")
    Sponge.getEventManager.post(AttributeLoadEvent)
    attributes = AttributeLoadEvent.list.asScala.toList
    AttributeLoadEvent.list.clear()
  }

  listenTo[Attribute.LoadEvent] { event =>
    event.register(new TemplateAttribute)
    event.register(new DurabilityAttribute)
  }
}
