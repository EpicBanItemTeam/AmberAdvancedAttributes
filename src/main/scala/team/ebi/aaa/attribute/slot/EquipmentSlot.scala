package team.ebi.aaa.attribute.slot

import org.spongepowered.api.Sponge
import org.spongepowered.api.data.manipulator.DataManipulator
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.item.inventory.equipment.EquipmentType
import team.ebi.aaa.api.data.{Template, TemplateSlot}
import team.ebi.aaa.attribute.AttributeManager
import team.ebi.aaa.data.CustomTemplates
import team.ebi.aaa.util._

class EquipmentSlot(manager: AttributeManager, equipment: EquipmentType) extends TemplateSlot.Equipment {
  override def toString: String = s"EquipmentSlot{$asTemplate}"

  override def getEquipmentType: EquipmentType = equipment

  override def asTemplate: Template = Template.parse(equipment.getId.replace('_', '-'))

  override def getTemplates(user: User): java.util.List[_ <: Template] = {
    Sponge.getCauseStackManager.pushCause(user)
    withData(user)(d => d.value.templates.asJava)
  }

  override def setTemplates(user: User, templates: java.util.List[_ <: Template]): Unit = {
    Sponge.getCauseStackManager.pushCause(user)
    withData(user)(d => d.value = CustomTemplates.Value(d.value.uuid, d.value.backup, templates.asScala.toList))
  }

  private def withData[T](user: User)(func: CustomTemplates.Data => T): T = {
    val item = get(user)
    val newItem = item.copy()
    val dataOption = item.get(classOf[CustomTemplates.Data])
    val data = if (dataOption.isPresent) dataOption.get else manager.unreachable(item)
    try func(data) finally {
      val result = newItem.offer(data.asInstanceOf[DataManipulator[_, _]])
      if (!result.isSuccessful) manager.unreachable(item) else if (!item.equalTo(newItem)) set(user, newItem)
    }
  }

  private def get(user: User): ItemStack = user.getEquipped(equipment).orElse(ItemStack.empty)

  private def set(user: User, item: ItemStack): Unit = if (!user.equip(equipment, item)) manager.unreachable(item)
}
