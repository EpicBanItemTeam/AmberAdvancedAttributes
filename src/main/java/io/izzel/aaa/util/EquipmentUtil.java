package io.izzel.aaa.util;

import com.google.common.collect.ImmutableList;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.data.StringValue;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.Attributes;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.util.Tuple;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class EquipmentUtil {

    public static final List<EquipmentType> EQUIPMENT_TYPES = ImmutableList.of(
            EquipmentTypes.HEADWEAR,
            EquipmentTypes.CHESTPLATE,
            EquipmentTypes.LEGGINGS,
            EquipmentTypes.BOOTS,
            EquipmentTypes.OFF_HAND,
            EquipmentTypes.MAIN_HAND
    );

    public static Stream<ItemStack> items(Equipable equipable) {
        return EQUIPMENT_TYPES.stream()
                .map(it -> Tuple.of(it, equipable.getEquipped(it)))
                .filter(tuple -> {
                    if (!tuple.getSecond().isPresent()) return false;
                    ImmutableList<StringValue> values = Attributes.EQUIPMENT.getValues(tuple.getSecond().get());
                    return values.isEmpty() || values.stream()
                            .map(it -> Sponge.getRegistry().getType(EquipmentType.class, it.getString()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .anyMatch(it -> it.equals(tuple.getFirst()));
                })
                .map(Tuple::getSecond).map(Optional::get);
    }

    public static <T extends RangeValue> double allOf(Equipable equipable, Attribute<T> attribute, double value) {
        double[] ret = {value};
        items(equipable)
                .map(attribute::getValues)
                .flatMap(Collection::stream)
                .map(it -> it.getFunction(ThreadLocalRandom.current()))
                .forEach(it -> ret[0] += it.applyAsDouble(ret[0]));
        return ret[0];
    }

    public static <T extends RangeValue> double allOf(Equipable equipable, Attribute<T> attribute) {
        return allOf(equipable, attribute, 0D);
    }

}