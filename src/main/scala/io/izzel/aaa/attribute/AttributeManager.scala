package io.izzel.aaa.attribute

import java.util.Optional

import com.github.benmanes.caffeine.cache.{CacheLoader, Caffeine, LoadingCache}
import com.google.common.collect.Lists
import com.google.inject.{Inject, Injector, Singleton}
import io.izzel.aaa.api.data._
import io.izzel.aaa.api.{Attribute, AttributeService}
import io.izzel.aaa.attribute.impl._
import io.izzel.aaa.config.ConfigReloadEvent
import io.izzel.aaa.data.CustomTemplates
import io.izzel.aaa.slot.{EquipmentSlot, GlobalSlot}
import io.izzel.aaa.util._
import ninja.leaping.configurate.ConfigurationNode
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.data.DataQuery
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Event
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent
import org.spongepowered.api.event.game.state.{GamePostInitializationEvent, GameStartingServerEvent}
import org.spongepowered.api.event.network.ClientConnectionEvent
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes
import org.spongepowered.api.item.inventory.{ItemStack, ItemStackSnapshot}
import org.spongepowered.api.plugin.PluginContainer

import scala.util.continuations.reset

@Singleton
class AttributeManager @Inject()(implicit container: PluginContainer, injector: Injector, logger: Logger, loader: MappingLoader) extends AttributeService {
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

  private class RefreshValue(value: Map[TemplateSlot, Mappings]) {
    def asJava: java.util.Map[TemplateSlot, Mappings] = value.asJava

    def get(slot: TemplateSlot): Option[Mappings] = value.get(slot)

    def keys: Set[TemplateSlot] = value.keySet
  }

  private class RefreshEvent(targetPlayer: Player, mappings: RefreshValue) extends MappingsRefreshEvent {
    private val spongeCurrentCause: Cause = Sponge.getCauseStackManager.pushCause(targetPlayer).pushCause(mappings).getCurrentCause

    override def getTargetMappings(slot: TemplateSlot): Optional[Mappings] = mappings.get(slot).asJava

    override def getAvailableSlots: java.util.Set[_ <: TemplateSlot] = mappings.keys.asJava

    override def getTargetEntity: Player = targetPlayer

    override def getCause: Cause = spongeCurrentCause
  }

  private object GenMappings extends CacheLoader[Player, Map[TemplateSlot, Mappings]] {
    listenTo[ConfigReloadEvent](_ => cache.asMap.keySet.asScala.clone.foreach(collectMappings(_, refresh = true)))
    listenTo[ClientConnectionEvent.Disconnect](event => cache.invalidate(event.getTargetEntity))
    listenTo[ClientConnectionEvent.Join](event => cache.get(event.getTargetEntity))
    listenTo[ChangeEntityEquipmentEvent](event => event.getTargetEntity match {
      case player: Player => collectMappings(player, refresh = true)
      case _ => ()
    })

    override def load(key: Player): Map[TemplateSlot, Mappings] = {
      if (!Sponge.getServer.isMainThread) throw new IllegalStateException("Not loaded on main thread")
      val result = loader.load(attributes, templateSlots)(key)
      logger.info(f"Refreshed templates for ${key.getName}")
      post(new RefreshEvent(key, new RefreshValue(result)))
      result
    }
  }

  reset {
    waitFor[GamePostInitializationEvent]
    logger.info("Injecting service ...")
    Sponge.getServiceManager.setProvider(container, classOf[AttributeService], this)

    waitFor[GameStartingServerEvent]
    logger.info("Registering attributes ...")
    post(SlotLoadEvent)
    post(AttributeLoadEvent)
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
    val toBeRegistered: java.util.List[Attribute[_]] = event.getAttributesToBeRegistered
    // functional attributes come firstly
    toBeRegistered.add(0, injector.getInstance(classOf[DurabilityAttribute]))
    toBeRegistered.add(0, injector.getInstance(classOf[CustomInfoAttribute]))
    // conditional attributes come secondly
    toBeRegistered.add(injector.getInstance(classOf[EquipmentAttribute]))
    toBeRegistered.add(injector.getInstance(classOf[SuitAttribute]))
    // the priority of template attribute is the lowest
    toBeRegistered.add(injector.getInstance(classOf[TemplateAttribute]))
  }

  listenTo[SlotLoadEvent.type] { event =>
    event.register(new EquipmentSlot(this, EquipmentTypes.MAIN_HAND))
    event.register(new EquipmentSlot(this, EquipmentTypes.OFF_HAND))
    event.register(new EquipmentSlot(this, EquipmentTypes.HEADWEAR))
    event.register(new EquipmentSlot(this, EquipmentTypes.CHESTPLATE))
    event.register(new EquipmentSlot(this, EquipmentTypes.LEGGINGS))
    event.register(new EquipmentSlot(this, EquipmentTypes.BOOTS))
    event.register(new GlobalSlot())
  }

  def attributeMap: Map[String, Attribute[_]] = attributes

  def slotMap: Map[Template, TemplateSlot] = templateSlots

  def unreachable(item: ItemStack): Nothing = {
    val cause = Sponge.getCauseStackManager.getCurrentCause
    cause.first(classOf[Player]).asScala match {
      case Some(player) => throw new UnreachableSlotDataException(s"$item for ${player.getName} is not ready for templates")
      case None => throw new UnreachableSlotDataException(s"$item is not ready for templates")
    }
  }

  override def collectMappings(player: Player, refresh: Boolean): java.util.Map[TemplateSlot, Mappings] = {
    val cause = Sponge.getCauseStackManager.getCurrentCause
    cause.first(classOf[RefreshValue]).asScala match {
      case Some(cached) => cached.asJava
      case None => locally {
        if (refresh) cache.invalidate(player)
        cache.get(player).asJava
      }
    }
  }

  override def getAttributes: java.util.Collection[Attribute[_]] = attributes.asJava.values

  override def getAttribute(key: String): Optional[Attribute[_]] = attributes.get(key).asJava

  override def getSlots: java.util.Collection[_ <: TemplateSlot] = templateSlots.asJava.values

  override def getSlot(key: Template): Optional[TemplateSlot] = templateSlots.get(key).asJava

  override def getExtraData(item: ItemStack, key: String): ConfigurationNode = {
    val dataOption = item.get(classOf[CustomTemplates.Data])
    if (dataOption.isPresent) dataOption.get.extra(DataQuery.of(key)).copy() else unreachable(item)
  }

  override def getExtraData(item: ItemStackSnapshot, key: String): ConfigurationNode = {
    val dataOption = item.get(classOf[CustomTemplates.ImmutableData])
    if (dataOption.isPresent) dataOption.get.extra(DataQuery.of(key)).copy() else unreachable(item.createStack())
  }

  override def setExtraData(item: ItemStack, key: String, node: ConfigurationNode): Unit = {
    val dataOption = item.get(classOf[CustomTemplates.Data])
    val data = if (dataOption.isPresent) dataOption.get else unreachable(item)
    if (node.isVirtual) data.extra.remove(DataQuery.of(key)) else data.extra.put(DataQuery.of(key), node.copy())
  }
}
