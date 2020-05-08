package team.ebi.aaa.attribute.mappings

import com.github.benmanes.caffeine.cache.{CacheLoader, Caffeine, LoadingCache}
import com.google.inject.{Inject, Provider, Singleton}
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent
import org.spongepowered.api.event.network.ClientConnectionEvent
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa.api.data.{Mappings, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.attribute.event.{CacheRefreshEvent, CacheRefreshValue}
import team.ebi.aaa.config.ConfigReloadEvent
import team.ebi.aaa.util._

@Singleton
class MappingsCache @Inject()(implicit container: PluginContainer,
                              logger: Logger, manager: Provider[AttributeManager], loader: MappingsGenerator) {
  private val cache: LoadingCache[Player, Map[TemplateSlot, Mappings]] = Caffeine.newBuilder.weakKeys.build(Loader)

  private object Loader extends CacheLoader[Player, Map[TemplateSlot, Mappings]] {
    override def load(key: Player): Map[TemplateSlot, Mappings] = {
      if (!Sponge.getServer.isMainThread) throw new IllegalStateException("Not loaded on main thread")
      logger.info(f"Refreshing templates for player ${key.getName} (${key.getUniqueId})")
      val result = loader.generate(manager.get.attributeMap, manager.get.slotMap)(key)
      post(new CacheRefreshEvent(key, new CacheRefreshValue(result)))
      result
    }
  }

  listenTo[ChangeEntityEquipmentEvent.TargetPlayer](e => manager.get.collectMappings(e.getTargetEntity, refresh = true))
  listenTo[ConfigReloadEvent](_ => cached.toSeq.foreach(manager.get.collectMappings(_, refresh = true)))
  listenTo[ClientConnectionEvent.Disconnect](e => invalidate(e.getTargetEntity))
  listenTo[ClientConnectionEvent.Join](e => retrieve(e.getTargetEntity))

  def retrieve(key: Player): Map[TemplateSlot, Mappings] = cache.get(key)

  def invalidate(key: Player): Unit = cache.invalidate(key)

  def cached: Iterable[Player] = cache.asMap.keySet.asScala
}
