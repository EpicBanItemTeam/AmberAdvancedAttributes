package io.izzel.aaa.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.izzel.aaa.collector.AttributeCollector;
import io.izzel.aaa.data.MarkerValue;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.data.StringValue;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.Attributes;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.util.Tuple;

import java.util.*;
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

    public static Stream<Map.Entry<EquipmentType, ItemStack>> itemsWithSlot(Equipable equipable) {
        return EQUIPMENT_TYPES.stream()
                .map(it -> Tuple.of(it, equipable.getEquipped(it)))
                .filter(tuple -> {
                    if (!tuple.getSecond().isPresent()) return false;
                    List<MarkerValue> gem = new ArrayList<>();
                    AttributeCollector.of(tuple.getSecond().get()).collect(Attributes.INLAY_GEM, gem).submit();
                    if (!gem.isEmpty()) return false;
                    ImmutableList<StringValue> values = Attributes.EQUIPMENT.getValues(tuple.getSecond().get());
                    return values.isEmpty() || values.stream()
                            .map(it -> Sponge.getRegistry().getType(EquipmentType.class, it.getString()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .anyMatch(it -> it.equals(tuple.getFirst()));
                })
                .map(it -> Maps.immutableEntry(it.getFirst(), it.getSecond().get()));
    }

    public static Stream<ItemStack> items(Equipable equipable) {
        return itemsWithSlot(equipable).map(Map.Entry::getValue);
    }

    public static <T extends RangeValue> double allOf(Equipable equipable, Attribute<T> attribute, double value) {
        double result = value;
        List<T> collection = new ArrayList<>();
        Sponge.getCauseStackManager().pushCause(equipable);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Iterator<ItemStack> iterator = items(equipable).iterator(); iterator.hasNext(); collection.clear()) {
            if (AttributeCollector.of(iterator.next()).collect(attribute, collection).submit()) {
                for (T range : collection) {
                    result += range.getFunction(random).applyAsDouble(result);
                }
            }
        }
        return result;
    }

    public static <T extends RangeValue> double allOf(Equipable equipable, Attribute<T> attribute) {
        return allOf(equipable, attribute, 0D);
    }

    public static <T extends DataSerializable> boolean hasAny(Equipable equipable, Attribute<T> attribute) {
        List<T> collection = new ArrayList<>();
        Sponge.getCauseStackManager().pushCause(equipable);
        for (Iterator<ItemStack> iterator = items(equipable).iterator(); iterator.hasNext(); collection.clear()) {
            if (AttributeCollector.of(iterator.next()).collect(attribute, collection).submit()) {
                return true;
            }
        }
        return false;
    }
}
