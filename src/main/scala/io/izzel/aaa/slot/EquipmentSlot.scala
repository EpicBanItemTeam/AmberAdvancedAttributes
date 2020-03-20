package io.izzel.aaa.slot

import java.util.function.BiFunction

import io.izzel.aaa.api.data.{Template, TemplateSlot, UnreachableSlotException}
import io.izzel.aaa.data.CustomTemplates
import io.izzel.aaa.util._
import ninja.leaping.configurate.ConfigurationNode
import org.spongepowered.api.data.DataQuery
import org.spongepowered.api.data.manipulator.DataManipulator
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.item.inventory.equipment.EquipmentType

class EquipmentSlot(equipment: EquipmentType) extends TemplateSlot.Equipment {
  override def toString: String = s"EquipmentSlot{$asTemplate}"

  override def getEquipmentType: EquipmentType = equipment

  override def asTemplate: Template = Template.parse(equipment.getId.replace('_', '-'))

  override def getTemplates(player: Player): java.util.List[_ <: Template] = {
    withData(player) { (data, _) =>
      data.value.templates.asJava
    }
  }

  override def setTemplates(player: Player, templates: java.util.List[_ <: Template]): Unit = {
    withData(player) { (data, _) =>
      data.value = CustomTemplates.Value(data.value.uuid, data.value.backup, templates.asScala.toList)
    }
  }

  override def withExtraData[T](player: Player, key: String, func: BiFunction[ConfigurationNode, ItemStack, T]): T = {
    withData(player) { (data, item) =>
      val query = DataQuery.of(key)
      val node = data.extra(query).copy()
      try func(node, item) finally {
        if (node.isVirtual) data.extra.remove(query) else data.extra.put(query, node.copy())
      }
    }
  }

  private def withData[T](player: Player)(func: (CustomTemplates.Data, ItemStack) => T): T = {
    val item = get(player)
    val newItem = item.copy()
    val dataOption = item.get(classOf[CustomTemplates.Data])
    val data = if (dataOption.isPresent) dataOption.get else unreachable(player, item)
    try func(data, newItem) finally {
      val result = newItem.offer(data.asInstanceOf[DataManipulator[_, _]])
      if (!result.isSuccessful) unreachable(player, item) else if (!item.equalTo(newItem)) set(player, item)
    }
  }

  private def set(player: Player, item: ItemStack): Unit = if (!player.equip(equipment, item)) unreachable(player, item)

  private def get(player: Player): ItemStack = player.getEquipped(equipment).orElse(ItemStack.empty)

  private def unreachable(player: Player, item: ItemStack): Nothing = {
    throw new UnreachableSlotException(s"$item in ${asTemplate()} of ${player.getName} is not ready for templates")
  }
}
