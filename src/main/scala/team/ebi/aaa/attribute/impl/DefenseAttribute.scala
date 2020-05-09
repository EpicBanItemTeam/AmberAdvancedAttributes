package team.ebi.aaa.attribute.impl

import com.google.inject.{Inject, Singleton}
import org.spongepowered.api.entity.Entity
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa.api.data.{Mappings, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.util._

@Singleton
class DefenseAttribute @Inject()(manager: AttributeManager)(implicit container: PluginContainer) extends traits.DefenseAttribute {
  override def getDeserializationKey: String = "aaa-defense"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = true

  override implicit def pluginContainer: PluginContainer = container

  override def getMappings(source: Entity, target: Player): Iterable[(TemplateSlot, Mappings)] = {
    manager.collectMappings(target).asScala
  }
}
