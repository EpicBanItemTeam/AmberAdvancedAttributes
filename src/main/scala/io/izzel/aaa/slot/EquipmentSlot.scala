package io.izzel.aaa.slot

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
  override def toString: String = s"EquipmentSlot{$equipment}"

  override def getEquipmentType: EquipmentType = equipment

  override def asTemplate(): Template = Template.parse(equipment.getId.replace('_', '-'))

  override def getTemplates(player: Player): java.util.List[_ <: Template] = {
    val item = get(player)
    val data = item.get(classOf[CustomTemplates.Data])
    val CustomTemplates.Value(_, _, templates) = if (data.isPresent) data.get.value else unreachable(player, item)
    templates.asJava
  }

  override def setTemplates(player: Player, templates: java.util.List[_ <: Template]): Unit = {
    val item = get(player)
    val data = item.get(classOf[CustomTemplates.Data])
    val CustomTemplates.Value(uuid, backup, _) = if (data.isPresent) data.get.value else unreachable(player, item)
    data.get.value = CustomTemplates.Value(uuid, backup, templates.asScala.toList)
    val result = item.offer(data.get.asInstanceOf[DataManipulator[_, _]])
    if (!result.isSuccessful) unreachable(player, item)
    set(player, item)
  }

  override def getExtraData(player: Player, key: String): ConfigurationNode = {
    val item = get(player)
    val data = item.get(classOf[CustomTemplates.Data])
    if (data.isPresent) data.get.extra(DataQuery.of(key)).copy() else unreachable(player, item)
  }

  override def setExtraData(player: Player, key: String, node: ConfigurationNode): Unit = {
    val item = get(player)
    val data = item.get(classOf[CustomTemplates.Data])
    val extra = if (data.isPresent) data.get.extra else unreachable(player, item)
    if (node.isVirtual) extra.remove(DataQuery.of(key)) else extra.put(DataQuery.of(key), node.copy())
    val result = item.offer(data.get.asInstanceOf[DataManipulator[_, _]])
    if (!result.isSuccessful) unreachable(player, item)
    set(player, item)
  }

  private def set(player: Player, item: ItemStack): Unit = if (!player.equip(equipment, item)) unreachable(player, item)

  private def get(player: Player): ItemStack = player.getEquipped(equipment).orElse(ItemStack.empty)

  private def unreachable(player: Player, item: ItemStack): Nothing = {
    throw new UnreachableSlotException(s"$item in ${asTemplate()} of ${player.getName} is not ready for templates")
  }
}
