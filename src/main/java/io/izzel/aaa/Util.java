package io.izzel.aaa;

import com.google.common.collect.ImmutableList;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Util {

    private static final List<EquipmentType> TYPES = ImmutableList.of(
        EquipmentTypes.HEADWEAR,
        EquipmentTypes.CHESTPLATE,
        EquipmentTypes.LEGGINGS,
        EquipmentTypes.BOOTS,
        EquipmentTypes.OFF_HAND,
        EquipmentTypes.MAIN_HAND
    );

    public static Stream<ItemStack> items(Equipable equipable) {
        return TYPES.stream().map(equipable::getEquipped).filter(Optional::isPresent).map(Optional::get);
    }

}
