package team.ebi.aaa.attribute.impl.traits

import java.util.Collections
import java.util.function.DoubleUnaryOperator

import org.spongepowered.api.entity.Entity
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.entity.projectile.Projectile
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource
import org.spongepowered.api.event.cause.entity.damage.{DamageModifier, DamageModifierType, DamageModifierTypes}
import org.spongepowered.api.event.entity.DamageEntityEvent
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa.api.Attribute
import team.ebi.aaa.api.data.{Mappings, TemplateSlot}
import team.ebi.aaa.data.DoubleRange
import team.ebi.aaa.data.DoubleRange.{Absolute, Relative}
import team.ebi.aaa.util.{listenTo, _}

import scala.util.Random

trait AttackAttribute extends Attribute[DoubleRange.Value[_]] {
  override def getDataClass: Class[DoubleRange.Value[_]] = classOf[DoubleRange.Value[_]]

  def getSourcePlayer(source: EntityDamageSource): Option[Player] = source.getSource match {
    case entity: Projectile => getSourcePlayer(EntityDamageSource.builder.from(source).entity(entity).build())
    case entity: Player => Some(entity)
    case _ => None
  }

  def randAmount(lower: Double, upper: Double, random: Random): Double = {
    lower + (upper - lower) * (if (random.nextBoolean()) random.nextDouble() else 1 - random.nextDouble())
  }

  def getModifierFunction(range: DoubleRange.Value[_]): DoubleUnaryOperator = range match {
    case DoubleRange.Value(Absolute(lower), Absolute(upper)) => new DoubleUnaryOperator {
      val amount: Double = randAmount(lower, upper, Random)

      override def applyAsDouble(operand: Double): Double = amount
    }
    case DoubleRange.Value(Relative(lower), Relative(upper)) => new DoubleUnaryOperator {
      val amount: Double = randAmount(lower, upper, Random)

      override def applyAsDouble(operand: Double): Double = operand * amount
    }
  }

  def getMappings(source: Player, target: Entity): Iterable[(TemplateSlot, Mappings)]

  implicit def pluginContainer: PluginContainer

  listenTo[DamageEntityEvent] { event =>
    for (source <- event.getCause.first(classOf[EntityDamageSource]).asScala; entity <- getSourcePlayer(source)) {
      for ((slot, mappings) <- getMappings(entity, event.getTargetEntity)) {
        val item = slot match {
          case slot: TemplateSlot.Equipment => entity.getEquipped(slot.getEquipmentType).orElse(ItemStack.empty)
          case _ => ItemStack.empty
        }
        val dataStream = Mappings.flattenDataStream(mappings, this)
        val modifierType = DamageModifierTypes.WEAPON_ENCHANTMENT
        for (data <- dataStream.iterator.asScala) {
          val builder = if (item.isEmpty) DamageModifier.builder else DamageModifier.builder.item(item)
          val modifier = builder.`type`(modifierType).cause(event.getCause.`with`(this, Nil: _*)).build()
          event.addModifierAfter(modifier, getModifierFunction(data), Collections.emptySet[DamageModifierType])
        }
      }
    }
  }
}
