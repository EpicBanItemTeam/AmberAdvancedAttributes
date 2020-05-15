package team.ebi.aaa.attribute.impl
import com.google.inject.{Inject, Singleton}
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource
import org.spongepowered.api.event.entity.DamageEntityEvent
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa.api.data.{Mappings, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.util._

import scala.util.Random

@Singleton
class FireBurningAttribute @Inject()(manager: AttributeManager)
                                    (implicit container: PluginContainer) extends traits.DoubleRangeAttribute {
  override def getDeserializationKey: String = "aaa-fire-burning"

  override def isCompatibleWith(slot: TemplateSlot): Boolean = true

  listenTo[DamageEntityEvent] { event =>
    for (source <- event.getCause.first(classOf[EntityDamageSource]).asScala) {
      val data = manager.collectMappings(source).values.asScala.flatMap(Mappings.flattenDataStream(_, this).iterator.asScala)
      val fireBurningTick = data.foldLeft(1.0)((input, range) => range(Random).apply(input)).floor.toInt - 1
      if (fireBurningTick > 0) {
        val oldFireBurningTick = Int.unbox(event.getTargetEntity.getOrElse(Keys.FIRE_TICKS, Int.box(0)))
        event.getTargetEntity.offer(Keys.FIRE_TICKS, Int.box(oldFireBurningTick + fireBurningTick))
      }
    }
  }
}
