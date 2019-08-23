package io.izzel.aaa.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import io.izzel.aaa.byteitems.ByteItemsHandler;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.data.StringValue;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.AttributeService;
import io.izzel.aaa.service.Attributes;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.util.Tuple;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
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

    public static <T extends DataSerializable> boolean hasAny(Equipable equipable, Attribute<T> attribute) {
        return items(equipable).map(attribute::getValues).flatMap(Collection::stream)
                .findAny().isPresent();
    }

    @SuppressWarnings("unchecked")
    public static <T extends DataSerializable> ImmutableList<T> getWithSuit(Equipable equipable, Attribute<T> attribute, ItemStack stack, ByteItemsHandler biHandler) {
        ImmutableList.Builder<T> builder = ImmutableList.builder();
        builder.addAll(attribute.getValues(stack));
        ImmutableList<StringValue> suits = Attributes.SUIT.getValues(stack);
        if (!suits.isEmpty()) {
            suits.stream().map(StringValue::getString)
                    .map(it -> Maps.immutableEntry(it, biHandler.read(it).createStack()))
                    .map(it -> getSuitAttributesIfAvailable(equipable, it.getValue(), it.getKey(), biHandler))
                    .flatMap(Streams::stream)
                    .forEach(it -> builder.addAll((List<T>) it));
        }
        return builder.build();
    }

    public static Optional<List<?>> getSuitAttributesIfAvailable(Equipable equipable, ItemStack suitItem, String suitId, ByteItemsHandler biHandler) {
        boolean available = Attributes.EQUIPMENT.getValues(suitItem).stream()
                .flatMap(eq -> Streams.stream(Sponge.getRegistry().getType(EquipmentType.class, eq.getString())))
                .map(equipable::getEquipped)
                .allMatch(it -> it.isPresent() && Attributes.SUIT.getValues(it.get()).stream().anyMatch(s -> s.getString().equals(suitId)));
        if (available)
            return Optional.of(AttributeService.instance().getAttributes()
                    .values()
                    .stream()
                    .map(it -> getWithSuit(equipable, it, suitItem, biHandler))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList()));
        else return Optional.empty();
    }

}
