package team.ebi.aaa.attribute.mappings

import java.util.UUID

import com.github.benmanes.caffeine.cache.{CacheLoader, Caffeine, LoadingCache}
import com.google.inject.{Inject, Provider, Singleton}
import io.izzel.amber.commons.i18n.AmberLocale
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent
import org.spongepowered.api.event.network.ClientConnectionEvent
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.service.user.UserStorageService
import team.ebi.aaa.api.data.{Mappings, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.attribute.event.{CacheRefreshEvent, CacheRefreshValue}
import team.ebi.aaa.config.ConfigReloadEvent
import team.ebi.aaa.util._

@Singleton
class MappingsCache @Inject()(implicit container: PluginContainer,
                              locale: AmberLocale, manager: Provider[AttributeManager], loader: MappingsGenerator) {
  private def userStorage: UserStorageService = Sponge.getServiceManager.provideUnchecked(classOf[UserStorageService])

  private val cache: LoadingCache[UUID, Map[TemplateSlot, Mappings]] = Caffeine.newBuilder.weakKeys.build(Loader)

  private object Loader extends CacheLoader[UUID, Map[TemplateSlot, Mappings]] {
    private def load(key: UUID, user: User): Map[TemplateSlot, Mappings] = {
      if (!Sponge.getServer.isMainThread) throw new IllegalStateException("Not loaded on main thread")
      val result = loader.generate(manager.get.attributeMap, manager.get.slotMap)(user)
      post(new CacheRefreshEvent(user, new CacheRefreshValue(result)))
      locale.log("log.mappings.refreshed", user.getName, key)
      result
    }

    override def load(key: UUID): Map[TemplateSlot, Mappings] = load(key, userStorage.get(key).get)
  }

  listenTo[ChangeEntityEquipmentEvent.TargetPlayer](e => manager.get.collectMappings(e.getTargetEntity, refresh = true))
  listenTo[ConfigReloadEvent](_ => cachedUsers.toSeq.foreach(manager.get.collectMappings(_, refresh = true)))
  listenTo[ClientConnectionEvent.Disconnect](e => invalidate(e.getTargetEntity))
  listenTo[ClientConnectionEvent.Login](e => retrieve(e.getTargetUser))

  def cachedUsers: Iterable[User] = cache.asMap.keySet.asScala.flatMap(userStorage.get(_).asScala)

  def retrieve(user: User): Map[TemplateSlot, Mappings] = cache.get(user.getProfile.getUniqueId)

  def invalidate(user: User): Unit = cache.invalidate(user.getProfile.getUniqueId)
}
