package team.ebi.aaa.attribute.impl

import com.google.inject.Inject
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa.api.data.{Mappings, MappingsRefreshEvent, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.util._

import scala.util.Random

class MaxHealthAttribute @Inject()(manager: AttributeManager)
                                  (implicit container: PluginContainer) extends traits.DoubleRangeAttribute {
  override def getDeserializationKey: String = "aaa-max-health"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = true

  private def handle(player: Player): Unit = {
    val mappings = manager.collectMappings(player).values.asScala
    val data = mappings.flatMap(Mappings.flattenDataStream(_, this).iterator.asScala)
    val maxHealth = data.foldLeft(20.0)((input, rangeValue) => rangeValue.average.apply(Random)(input))
    player.offer(Keys.MAX_HEALTH, Double.box(maxHealth)) // I don't know how to get the default maximum health
  }

  listenTo[MappingsRefreshEvent](event => event.getTargetUser.getPlayer.asScala.foreach(handle))
}
