package team.ebi.aaa.attribute.mappings

import java.util.UUID

import com.google.inject.{Inject, Provider, Singleton}
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.entity.projectile.Projectile
import org.spongepowered.api.event.cause.EventContextKeys
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes
import org.spongepowered.api.event.entity.{DestructEntityEvent, SpawnEntityEvent}
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa.api.data.{Mappings, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.util._

import scala.collection.mutable

@Singleton
class ProjectileMappingsCache @Inject()(implicit container: PluginContainer, manager: Provider[AttributeManager]) {
  private val cache: mutable.Map[UUID, Map[TemplateSlot, Mappings]] = mutable.WeakHashMap()

  listenTo[DestructEntityEvent] { event =>
    cache.remove(event.getTargetEntity.getUniqueId)
  }

  listenTo[SpawnEntityEvent] { event =>
    if (event.getContext.get(EventContextKeys.SPAWN_TYPE).asScala.contains(SpawnTypes.PROJECTILE)) {
      event.getContext.get(EventContextKeys.OWNER).asScala match {
        case Some(player: Player) => locally {
          val mappings = manager.get.collectMappings(player).asScala.toMap
          event.getEntities.asScala.foreach(entity => cache.put(entity.getUniqueId, mappings))
        }
        case _ => ()
      }
    }
  }

  def retrieve(entity: Projectile): Map[TemplateSlot, Mappings] = cache.getOrElse(entity.getUniqueId, Map.empty)
}
