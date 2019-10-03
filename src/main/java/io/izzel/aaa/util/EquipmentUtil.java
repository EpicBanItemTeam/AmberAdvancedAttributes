package io.izzel.aaa.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.izzel.aaa.collector.AttributeCollector;
import io.izzel.aaa.data.MarkerValue;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.data.StringValue;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.Attributes;
import io.izzel.aaa.service.EquipmentSlotService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Tuple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class EquipmentUtil {

    public static Stream<Map.Entry<String, ItemStack>> itemsWithSlot(Equipable equipable) {
        var service = Sponge.getServiceManager().provideUnchecked(EquipmentSlotService.class);
        return service.slots().stream()
                .map(it -> Tuple.of(it, service.getItemStack(equipable, it)))
                .filter(tuple -> {
                    // todo 将判断物品有效的逻辑迁移至 AttributeCollector
                    if (!tuple.getSecond().isPresent()) return false;
                    List<MarkerValue> gem = Attributes.INLAY_GEM.getValues(tuple.getSecond().get());
                    if (!gem.isEmpty()) return false;
                    var values = Attributes.EQUIPMENT.getValues(tuple.getSecond().get());
                    return values.isEmpty() || values.stream()
                            .map(StringValue::getString)
                            .anyMatch(it -> it.equals(tuple.getFirst()));
                })
                .map(it -> Maps.immutableEntry(it.getFirst(), it.getSecond().get()));
    }

    public static Stream<ItemStack> items(Equipable equipable) {
        return itemsWithSlot(equipable).map(Map.Entry::getValue);
    }

    public static <T extends RangeValue> double allOf(Equipable equipable, Attribute<T> attribute, double value) {
        var result = value;
        List<T> collection = new ArrayList<>();
        Sponge.getCauseStackManager().pushCause(equipable);
        var random = ThreadLocalRandom.current();
        for (var iterator = items(equipable).iterator(); iterator.hasNext(); collection.clear()) {
            if (AttributeCollector.of(iterator.next()).collect(attribute, collection).submit()) {
                for (var range : collection) {
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
        for (var iterator = items(equipable).iterator(); iterator.hasNext(); collection.clear()) {
            if (AttributeCollector.of(iterator.next()).collect(attribute, collection).submit()) {
                return true;
            }
        }
        return false;
    }
}
