package team.ebi.aaa.attribute.impl.traits

import org.spongepowered.api.entity.Entity
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource
import org.spongepowered.api.event.entity.DamageEntityEvent
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa.api.data.{Mappings, TemplateSlot}
import team.ebi.aaa.util._

import scala.util.Random

trait ApplyBeforeAttribute extends DoubleRangeAttribute {
  implicit def pluginContainer: PluginContainer

  def getMappings(source: EntityDamageSource, target: Entity): Iterable[(TemplateSlot, Mappings)]

  def on(event: DamageEntityEvent): Unit = {
    for (source <- event.getCause.first(classOf[EntityDamageSource]).asScala) {
      for ((_, mappings) <- getMappings(source, event.getTargetEntity)) {
        val dataStream = Mappings.flattenDataStream(mappings, this)
        for (data <- dataStream.iterator.asScala) {
          event.setBaseDamage(data(Random)(event.getBaseDamage))
        }
      }
    }
  }
}
