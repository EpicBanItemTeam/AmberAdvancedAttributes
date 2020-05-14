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
class DefensePVPAttribute @Inject()(manager: AttributeManager)(implicit container: PluginContainer) extends traits.ApplyAfterAttribute {
  override def getDeserializationKey: String = "aaa-pvp-defense"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = true

  override implicit def pluginContainer: PluginContainer = container

  override def getMappings(source: EntityDamageSource, target: Entity): Iterable[(TemplateSlot, Mappings)] = target match {
    case player: Player => if (source.isInstanceOf[Player]) manager.collectMappings(player).asScala else Nil
    case _ => Nil
  }
}
