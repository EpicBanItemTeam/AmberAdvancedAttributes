package io.izzel.aaa.service;

import com.google.inject.ImplementedBy;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@ImplementedBy(EquipmentSlotServiceImpl.class)
public interface EquipmentSlotService {

    List<String> slots();

    Optional<ItemStack> getItemStack(Equipable equipable, String slot);

    boolean setItemStack(Equipable equipable, String slot, ItemStack itemStack);

    interface RegistryEvent extends Event {

        void register(String id, Function<Equipable, Optional<ItemStack>> getter, BiFunction<Equipable, ItemStack, Boolean> setter);

    }

}
