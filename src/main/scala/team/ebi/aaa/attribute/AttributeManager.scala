package team.ebi.aaa.attribute

import com.google.inject.{Inject, Injector, Singleton}
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.event.game.state.{GamePostInitializationEvent, GameStartingServerEvent}
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa
import team.ebi.aaa.api.data._
import team.ebi.aaa.api.{Attribute, AttributeService}
import team.ebi.aaa.attribute.event.{AttributeLoadEvent, TemplateSlotLoadEvent}
import team.ebi.aaa.attribute.impl._
import team.ebi.aaa.attribute.mappings.MappingsCache
import team.ebi.aaa.attribute.slot.{EquipmentSlot, GlobalSlot}
import team.ebi.aaa.util._

import scala.collection.immutable
import scala.util.continuations.reset

@Singleton
class AttributeManager @Inject()(implicit container: PluginContainer,
                                 injector: Injector, logger: Logger, cache: MappingsCache) extends AttributeServiceImpl {
  private var attributes: immutable.ListMap[String, Attribute[_]] = immutable.ListMap.empty

  private var templateSlots: immutable.ListMap[Template, TemplateSlot] = immutable.ListMap.empty

  private def registerAttributes(): Unit = {
    logger.info("Registering attributes ...")
    val event = new AttributeLoadEvent(logger)
    if (!post(event)) attributes = event.build()
    // the priority of template attribute is the lowest
    attributes += aaa.templateKey -> injector.getInstance(classOf[TemplateAttribute])
    logger.info(s"Registered attributes: ${attributes.keys.mkString("[", ", ", "]")}")
  }

  private def registerTemplateSlots(): Unit = {
    logger.info("Registering template slots ...")
    post(new TemplateSlotLoadEvent(templateSlots += _ -> _))
    logger.info(s"Registered template slots: ${templateSlots.keys.mkString("[", ", ", "]")}")
  }

  private def injectAttributeService(): Unit = {
    logger.info("Injecting attribute service ...")
    Sponge.getServiceManager.setProvider(container, classOf[AttributeService], this)
  }

  reset {
    waitFor[GamePostInitializationEvent]
    injectAttributeService()

    waitFor[GameStartingServerEvent]
    registerTemplateSlots()
    registerAttributes()
  }

  listenTo[Attribute.LoadEvent] { event =>
    val toBeRegistered: java.util.List[Attribute[_]] = event.getAttributesToBeRegistered
    // functional attributes come firstly
    toBeRegistered.add(0, injector.getInstance(classOf[AttackAttribute]))
    toBeRegistered.add(0, injector.getInstance(classOf[AttackPVEAttribute]))
    toBeRegistered.add(0, injector.getInstance(classOf[AttackPVPAttribute]))
    toBeRegistered.add(0, injector.getInstance(classOf[CustomInfoAttribute]))
    toBeRegistered.add(0, injector.getInstance(classOf[DurabilityAttribute]))
    toBeRegistered.add(0, injector.getInstance(classOf[HelloAttribute]))
    // conditional attributes come secondly
    toBeRegistered.add(injector.getInstance(classOf[EquipmentAttribute]))
    toBeRegistered.add(injector.getInstance(classOf[SuitAttribute]))
    // the priority of template attribute is the lowest
    // so it is added manually
  }

  listenTo[TemplateSlotLoadEvent] { event =>
    event.register(new EquipmentSlot(this, EquipmentTypes.MAIN_HAND))
    event.register(new EquipmentSlot(this, EquipmentTypes.OFF_HAND))
    event.register(new EquipmentSlot(this, EquipmentTypes.HEADWEAR))
    event.register(new EquipmentSlot(this, EquipmentTypes.CHESTPLATE))
    event.register(new EquipmentSlot(this, EquipmentTypes.LEGGINGS))
    event.register(new EquipmentSlot(this, EquipmentTypes.BOOTS))
    event.register(new GlobalSlot())
  }

  def collect(user: User, refresh: Boolean): Map[TemplateSlot, Mappings] = {
    if (refresh) cache.invalidate(user)
    cache.retrieve(user)
  }

  def attributeMap: Map[String, Attribute[_]] = attributes

  def slotMap: Map[Template, TemplateSlot] = templateSlots

  def unreachable(item: ItemStack): Nothing = {
    val cause = Sponge.getCauseStackManager.getCurrentCause
    cause.first(classOf[User]).asScala match {
      case Some(user) => throw new UnreachableSlotDataException(s"$item for ${user.getName} is not ready for templates")
      case None => throw new UnreachableSlotDataException(s"$item is not ready for templates")
    }
  }
}
