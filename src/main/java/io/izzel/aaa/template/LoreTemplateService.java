package io.izzel.aaa.template;

import com.google.inject.ImplementedBy;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.Text;

import java.util.List;

@ImplementedBy(LoreTemplateServiceImpl.class)
public interface LoreTemplateService {

    List<Text> eval(String name, Equipable entity, ItemStack itemStack);

    List<Text> eval(String name, Equipable entity, ItemStackSnapshot itemStack);

    static LoreTemplateService instance() {
        return Sponge.getServiceManager().provideUnchecked(LoreTemplateService.class);
    }

}
