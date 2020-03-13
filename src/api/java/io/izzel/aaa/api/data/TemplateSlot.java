package io.izzel.aaa.api.data;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;

import java.util.List;

public interface TemplateSlot {
    Template asTemplate();

    List<? extends Template> getTemplates(Player player) throws UnreachableSlotException;

    void setTemplates(Player player, List<? extends Template> templates) throws UnreachableSlotException;

    interface Equipment extends TemplateSlot {
        EquipmentType getEquipmentType();
    }
}
