package team.ebi.aaa.attribute.impl

import com.google.inject.Inject
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa.api.Attribute
import team.ebi.aaa.api.data.{Mappings, MappingsRefreshEvent, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.data.DoubleRange
import team.ebi.aaa.data.DoubleRange.{Absolute, Relative}
import team.ebi.aaa.util._

class MaxHealthAttribute @Inject()(manager: AttributeManager)
                                  (implicit container: PluginContainer) extends Attribute[DoubleRange.Value[_]] {
  override def getDataClass: Class[DoubleRange.Value[_]] = classOf[DoubleRange.Value[_]]

  override def getDeserializationKey: String = "aaa-max-health"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = true

  private def handle(player: Player): Unit = {
    var maxHealth = 20.0 // I don't know how to get the default health
    val mappingsCollection = manager.collectMappings(player).values.asScala
    mappingsCollection.flatMap(Mappings.flattenDataStream(_, this).iterator.asScala).foreach {
      case DoubleRange.Value(Relative(lower), Relative(upper)) => maxHealth *= math.max(0, 1 + (lower + upper) / 2)
      case DoubleRange.Value(Absolute(lower), Absolute(upper)) => maxHealth += math.max(-maxHealth, (lower + upper) / 2)
    }
    player.offer(Keys.MAX_HEALTH, Double.box(maxHealth))
  }

  listenTo[MappingsRefreshEvent](event => event.getTargetUser.getPlayer.asScala.foreach(handle))
}
