package io.izzel.aaa.api.template;

import org.spongepowered.api.entity.living.player.Player;

import java.util.List;

public interface TemplateSlot {
    AttributeTemplate asTemplate();

    List<? extends AttributeTemplate> getTemplates(Player player) throws UnreachableSlotException;

    void setTemplates(Player player, List<? extends AttributeTemplate> templates) throws UnreachableSlotException;
}
