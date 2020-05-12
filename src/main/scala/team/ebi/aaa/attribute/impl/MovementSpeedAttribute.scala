package team.ebi.aaa.attribute.impl

import com.google.inject.Inject
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa.api.data.{Mappings, MappingsRefreshEvent, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.util._

import scala.util.Random

class MovementSpeedAttribute @Inject()(manager: AttributeManager)
                                      (implicit container: PluginContainer) extends traits.DoubleRangeAttribute {
  override def getDeserializationKey: String = "aaa-movement-speed"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = true

  private def handle(player: Player): Unit = {
    val mappings = manager.collectMappings(player).values.asScala
    val data = mappings.flatMap(Mappings.flattenDataStream(_, this).iterator.asScala)
    val speed = data.foldLeft(1.0)((input, rangeValue) => rangeValue.average.apply(Random)(input))
    player.offer(Keys.FLYING_SPEED, Double.box(0.05 * speed)) // I don't know how to get the default flying speed
    player.offer(Keys.WALKING_SPEED, Double.box(0.1 * speed)) // I don't know how to get the default walking speed
  }

  listenTo[MappingsRefreshEvent](event => event.getTargetUser.getPlayer.asScala.foreach(handle))
}
