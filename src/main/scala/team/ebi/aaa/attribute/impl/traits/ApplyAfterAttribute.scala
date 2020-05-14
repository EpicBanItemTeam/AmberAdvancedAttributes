package team.ebi.aaa.attribute.impl.traits

import java.util.Collections
import java.util.function.DoubleUnaryOperator

import org.spongepowered.api.entity.projectile.Projectile
import org.spongepowered.api.entity.{Entity, Equipable}
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource
import org.spongepowered.api.event.cause.entity.damage.{DamageModifier, DamageModifierType, DamageModifierTypes}
import org.spongepowered.api.event.entity.DamageEntityEvent
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.plugin.PluginContainer
import team.ebi.aaa.api.data.{Mappings, TemplateSlot}
import team.ebi.aaa.util._

import scala.annotation.tailrec
import scala.util.Random

trait ApplyAfterAttribute extends DoubleRangeAttribute {
  implicit def pluginContainer: PluginContainer

  @tailrec
  // noinspection DuplicatedCode
  private def getRealEntity(target: AnyRef): Option[Entity] = target match {
    case entity: Projectile => getRealEntity(entity.getShooter)
    case entity: Entity => Some(entity)
    case _ => None
  }

  def getModifierFunction(function: Double => Double): DoubleUnaryOperator = new DoubleUnaryOperator {
    override def applyAsDouble(operand: Double): Double = operand / function(1) - operand
  }

  def getMappings(source: EntityDamageSource, target: Entity): Iterable[(TemplateSlot, Mappings)]

  def modifierType: DamageModifierType = DamageModifierTypes.SHIELD

  def on(event: DamageEntityEvent): Unit = {
    for (source <- event.getCause.first(classOf[EntityDamageSource]).asScala) {
      for ((slot, mappings) <- getMappings(source, event.getTargetEntity)) {
        val item = slot match {
          case slot: TemplateSlot.Equipment => source match {
            case source: Equipable => source.getEquipped(slot.getEquipmentType).orElse(ItemStack.empty)
            case _ => ItemStack.empty
          }
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
