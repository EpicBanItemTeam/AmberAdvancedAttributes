package team.ebi.aaa.attribute

import java.util.{Collections, Optional}

import ninja.leaping.configurate.ConfigurationNode
import org.spongepowered.api.Sponge
import org.spongepowered.api.data.DataQuery
import org.spongepowered.api.data.manipulator.DataManipulator
import org.spongepowered.api.entity.living.player.{Player, User}
import org.spongepowered.api.entity.projectile.Projectile
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource
import org.spongepowered.api.item.inventory.{ItemStack, ItemStackSnapshot}
import team.ebi.aaa.api.data.{Mappings, Template, TemplateSlot}
import team.ebi.aaa.api.{Attribute, AttributeService}
import team.ebi.aaa.attribute.event.CacheRefreshValue
import team.ebi.aaa.data.CustomTemplates
import team.ebi.aaa.util._

trait AttributeServiceImpl extends AttributeService {
  this: AttributeManager =>

  override def refreshMappings(user: User): Unit = {
    val cause = Sponge.getCauseStackManager.getCurrentCause
    if (!cause.first(classOf[CacheRefreshValue]).isPresent) cacheMap.tap(_.invalidate(user)).retrieve(user)
  }

  override def collectMappings(user: User): java.util.Map[TemplateSlot, Mappings] = {
    val cause = Sponge.getCauseStackManager.getCurrentCause
    cause.first(classOf[CacheRefreshValue]).asScala match {
      case None => cacheMap.retrieve(user).asJava
      case Some(cached) => cached.asJava
    }
  }

  override def collectMappings(source: EntityDamageSource): java.util.Map[TemplateSlot, Mappings] = source.getSource match {
    case entity: Projectile => projectileCacheMap.retrieve(entity).asJava
    case entity: Player => cacheMap.retrieve(entity).asJava
    case _ => Collections.emptyMap[TemplateSlot, Mappings]
  }

  override def getAttributes: java.util.Collection[Attribute[_]] = attributeMap.asJava.values

  override def getAttribute(key: String): Optional[Attribute[_]] = attributeMap.get(key).asJava

  override def getSlots: java.util.Collection[_ <: TemplateSlot] = slotMap.asJava.values

  override def getSlot(key: Template): Optional[TemplateSlot] = slotMap.get(key).asJava

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
    if (node.getValue == null) data.extra.remove(DataQuery.of(key)) else data.extra.put(DataQuery.of(key), node.copy())
    item.offer(data.asInstanceOf[DataManipulator[_, _]])
  }
}
