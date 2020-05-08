package team.ebi.aaa.slot

import team.ebi.aaa.api.data.{Template, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.data.CustomTemplates
import team.ebi.aaa.util._
import org.spongepowered.api.Sponge
import org.spongepowered.api.data.manipulator.DataManipulator
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.item.inventory.equipment.EquipmentType

class EquipmentSlot(manager: AttributeManager, equipment: EquipmentType) extends TemplateSlot.Equipment {
  override def toString: String = s"EquipmentSlot{$asTemplate}"

  override def getEquipmentType: EquipmentType = equipment

  override def asTemplate: Template = Template.parse(equipment.getId.replace('_', '-'))

  override def getTemplates(player: Player): java.util.List[_ <: Template] = {
    Sponge.getCauseStackManager.pushCause(player)
    withData(player)(d => d.value.templates.asJava)
  }

  override def setTemplates(player: Player, templates: java.util.List[_ <: Template]): Unit = {
    Sponge.getCauseStackManager.pushCause(player)
    withData(player)(d => d.value = CustomTemplates.Value(d.value.uuid, d.value.backup, templates.asScala.toList))
  }

  private def withData[T](player: Player)(func: CustomTemplates.Data => T): T = {
    val item = get(player)
    val newItem = item.copy()
    val dataOption = item.get(classOf[CustomTemplates.Data])
    val data = if (dataOption.isPresent) dataOption.get else manager.unreachable(item)
    try func(data) finally {
      val result = newItem.offer(data.asInstanceOf[DataManipulator[_, _]])
      if (!result.isSuccessful) manager.unreachable(item) else if (!item.equalTo(newItem)) set(player, newItem)
    }
  }

  private def get(player: Player): ItemStack = player.getEquipped(equipment).orElse(ItemStack.empty)

  private def set(player: Player, item: ItemStack): Unit = if (!player.equip(equipment, item)) manager.unreachable(item)
}
