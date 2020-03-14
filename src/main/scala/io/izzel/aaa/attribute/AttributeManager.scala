package io.izzel.aaa.attribute

import java.util.Optional

import com.github.benmanes.caffeine.cache.{CacheLoader, Caffeine, LoadingCache}
import com.google.common.collect.Lists
import com.google.inject.{Inject, Singleton}
import io.izzel.aaa.api.data.{Mappings, Template, TemplateSlot}
import io.izzel.aaa.api.{Attribute, AttributeService}
import io.izzel.aaa.slot.{EquipmentSlot, GlobalSlot}
import io.izzel.aaa.util.EventUtil._
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Event
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent
import org.spongepowered.api.event.game.state.{GamePostInitializationEvent, GameStartingServerEvent}
import org.spongepowered.api.event.network.ClientConnectionEvent
import org.spongepowered.api.item.inventory.equipment.{EquipmentType, EquipmentTypes}
import org.spongepowered.api.plugin.PluginContainer

import scala.collection.JavaConverters._
import scala.util.continuations.reset

@Singleton
class AttributeManager @Inject()(implicit container: PluginContainer, logger: Logger) extends AttributeService {
  private var attributes: Map[String, Attribute[_]] = Map.empty

  private var templateSlots: Map[Template, TemplateSlot] = Map.empty

  private val cache: LoadingCache[Player, Map[TemplateSlot, Mappings]] = Caffeine.newBuilder.weakKeys.build(GenMappings)

  private object AttributeLoadEvent extends Attribute.LoadEvent {
    // reverse the list since add(0, list) is called quite frequently so that it will be added as last element actually
    private[AttributeManager] val list: java.util.List[Attribute[_]] = Lists.reverse(new java.util.ArrayList)

    private val spongeCurrentCause: Cause = Sponge.getCauseStackManager.getCurrentCause

    override def getAttributesToBeRegistered: java.util.List[Attribute[_]] = list

    override def getCause: Cause = spongeCurrentCause
  }

  private object SlotLoadEvent extends Event { // TODO: expose to api
    private val spongeCurrentCause: Cause = Sponge.getCauseStackManager.getCurrentCause

    def register(slot: TemplateSlot): Unit = templateSlots += slot.asTemplate() -> slot

    override def getCause: Cause = spongeCurrentCause
  }

  private object GenMappings extends CacheLoader[Player, Map[TemplateSlot, Mappings]] {
    listenTo[ClientConnectionEvent.Disconnect](event => cache.invalidate(event.getTargetEntity))
    listenTo[ClientConnectionEvent.Join](event => cache.refresh(event.getTargetEntity))
    listenTo[ChangeEntityEquipmentEvent](event => event.getTargetEntity match {
      case player: Player => cache.refresh(player)
      case _ => ()
    })

    override def load(key: Player): Map[TemplateSlot, Mappings] = Map.empty // TODO: implementation
  }

  reset {
    waitFor[GamePostInitializationEvent]
    logger.info("Injecting service ...")
    Sponge.getServiceManager.setProvider(container, classOf[AttributeService], this)

    waitFor[GameStartingServerEvent]
    logger.info("Registering attributes ...")
    Sponge.getEventManager.post(SlotLoadEvent)
    Sponge.getEventManager.post(AttributeLoadEvent)
    attributes = AttributeLoadEvent.list.asScala.groupBy(_.getDeserializationKey).mapValues {
      case buffer if buffer.size > 1 => locally {
        val lastRegistered = buffer.head
        val key = lastRegistered.getDeserializationKey
        logger.warn(s"Duplicate deserialization key: $key (only the last registered will take effect)")
        lastRegistered
      }
      case buffer => buffer.last
    }
    AttributeLoadEvent.list.clear()
  }

  listenTo[Attribute.LoadEvent] { event =>
    event.register(new TemplateAttribute)
    event.register(new DurabilityAttribute)
  }

  listenTo[SlotLoadEvent.type] { event =>
    event.register(new EquipmentSlot(EquipmentTypes.MAIN_HAND))
    event.register(new EquipmentSlot(EquipmentTypes.OFF_HAND))
    event.register(new EquipmentSlot(EquipmentTypes.HEADWEAR))
    event.register(new EquipmentSlot(EquipmentTypes.CHESTPLATE))
    event.register(new EquipmentSlot(EquipmentTypes.LEGGINGS))
    event.register(new EquipmentSlot(EquipmentTypes.BOOTS))
    event.register(new GlobalSlot())
  }

  override def collectMappings(player: Player, refresh: Boolean): java.util.Map[TemplateSlot, Mappings] = {
    if (refresh) cache.refresh(player)
    cache.get(player).asJava
  }

  override def getAttributes: java.util.Collection[Attribute[_]] = attributes.asJava.values

  override def getAttribute(key: String): Optional[Attribute[_]] = Optional.ofNullable(attributes.get(key).orNull)
}
