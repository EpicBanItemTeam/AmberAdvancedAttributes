package team.ebi.aaa.api.data;

import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;

import java.util.List;

public interface TemplateSlot {
    Template asTemplate();

    List<? extends Template> getTemplates(User user) throws UnreachableSlotDataException;

    void setTemplates(User user, List<? extends Template> templates) throws UnreachableSlotDataException;

    interface Equipment extends TemplateSlot {
        EquipmentType getEquipmentType();
    }

    interface Global extends TemplateSlot {
        List<? extends Template> getTemplates();

        void setTemplates(List<? extends Template> templates);

        @Override
        default List<? extends Template> getTemplates(User user) {
            return this.getTemplates();
        }

        @Override
        default void setTemplates(User user, List<? extends Template> templates) {
            this.setTemplates(templates);
        }
    }
}
