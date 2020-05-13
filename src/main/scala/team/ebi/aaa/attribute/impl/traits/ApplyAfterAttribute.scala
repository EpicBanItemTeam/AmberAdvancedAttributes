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
import team.ebi.aaa.api.data.{Mappings, TemplateSlot}
import team.ebi.aaa.util.{listenTo, _}

import scala.annotation.tailrec
import scala.util.Random

trait ApplyAfterAttribute extends DoubleRangeAttribute {
  implicit def pluginContainer: PluginContainer

  @tailrec
  // noinspection DuplicatedCode
  private def getRealEntity(target: AnyRef): Option[Player] = target match {
    case entity: Projectile => getRealEntity(entity.getShooter)
    case entity: Player => Some(entity)
    case _ => None
  }

  def getModifierFunction(function: Double => Double): DoubleUnaryOperator = new DoubleUnaryOperator {
    override def applyAsDouble(operand: Double): Double = operand / function(1) - operand
  }

  def getMappings(source: Entity, target: Player): Iterable[(TemplateSlot, Mappings)]

  def modifierType: DamageModifierType = DamageModifierTypes.SHIELD

  listenTo[DamageEntityEvent] { event =>
    for (source <- event.getCause.first(classOf[EntityDamageSource]).asScala; entity <- getRealEntity(event.getTargetEntity)) {
      for ((slot, mappings) <- getMappings(getRealEntity(source.getSource).getOrElse(source.getSource), entity)) {
        val item = slot match {
          case slot: TemplateSlot.Equipment => entity.getEquipped(slot.getEquipmentType).orElse(ItemStack.empty)
          case _ => ItemStack.empty
        }
        val dataStream = Mappings.flattenDataStream(mappings, this)
        for (data <- dataStream.iterator.asScala) {
          val builder = if (item.isEmpty) DamageModifier.builder else DamageModifier.builder.item(item)
          val modifier = builder.`type`(modifierType).cause(event.getCause.`with`(this, Nil: _*)).build()
          event.addModifierAfter(modifier, getModifierFunction(data(Random)), Collections.emptySet[DamageModifierType])
        }
      }
    }
  }
}
