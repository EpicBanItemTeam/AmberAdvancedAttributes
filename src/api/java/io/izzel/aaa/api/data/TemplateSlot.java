package io.izzel.aaa.api.data;

import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;

import java.util.List;

public interface TemplateSlot {
    Template asTemplate();

    List<? extends Template> getTemplates(Player player) throws UnreachableSlotException;

    void setTemplates(Player player, List<? extends Template> templates) throws UnreachableSlotException;

    interface ContainsExtraData extends TemplateSlot {
        ConfigurationNode getExtraData(Player player, String key) throws UnreachableSlotException;

        void setExtraData(Player player, String key, ConfigurationNode node) throws UnreachableSlotException;
    }

    interface Equipment extends TemplateSlot, ContainsExtraData {
        EquipmentType getEquipmentType();

        @Override
        ConfigurationNode getExtraData(Player player, String key) throws UnreachableSlotException;

        @Override
        void setExtraData(Player player, String key, ConfigurationNode node) throws UnreachableSlotException;
    }

    interface Global extends TemplateSlot {
        List<? extends Template> getTemplates();

        void setTemplates(List<? extends Template> templates);

        @Override
        default List<? extends Template> getTemplates(Player player) {
            return this.getTemplates();
        }

        @Override
        default void setTemplates(Player player, List<? extends Template> templates) {
            this.setTemplates(templates);
        }
    }
}
