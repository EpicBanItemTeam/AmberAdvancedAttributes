package team.ebi.aaa.attribute.impl

import com.google.inject.{Inject, Singleton}
import org.spongepowered.api.entity.Entity
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa.api.data.{Mappings, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.util._

@Singleton
class AttackPVEAttribute @Inject()(manager: AttributeManager)(implicit container: PluginContainer) extends traits.ApplyBeforeAttribute {
  override def getDeserializationKey: String = "aaa-pve-attack"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = true

  override implicit def pluginContainer: PluginContainer = container

  override def getMappings(source: EntityDamageSource, target: Entity): Iterable[(TemplateSlot, Mappings)] = {
    if (target.isInstanceOf[Player]) Nil else manager.collectMappings(source).asScala
  }
}
