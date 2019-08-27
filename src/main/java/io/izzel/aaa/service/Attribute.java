package io.izzel.aaa.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Maps;
import io.izzel.aaa.collector.AttributeCollector;
import io.izzel.aaa.util.EquipmentUtil;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author ustc_zzzz yinyangshi
 */
@NonnullByDefault
public interface Attribute<T extends DataSerializable> {

    String getId();

    Class<T> getDataClass();

    ImmutableListMultimap<Byte, Text> getLoreTexts(List<? extends T> values, Equipable equipable);

    ImmutableList<T> getValues(ItemStack item);

    default ImmutableList<T> getAll(ItemStack item, Equipable owner) {
        if (owner instanceof Living) {
            List<T> collection = new ArrayList<>();
            AttributeCollector.of(item).collect(this, collection).submit();
            return ImmutableList.copyOf(collection);
        }
        return ImmutableList.of();
    }

    ImmutableList<T> getValues(ItemStackSnapshot item);

    void setValues(ItemStack item, List<? extends T> values);

    default void prependValue(ItemStack item, T value) {
        this.setValues(item, ImmutableList.<T>builder().add(value).addAll(this.getValues(item)).build());
    }

    default void appendValue(ItemStack item, T value) {
        this.setValues(item, ImmutableList.<T>builder().addAll(this.getValues(item)).add(value).build());
    }

    /**
     * insert the value to the index
     *
     * @param item  the item to insert value
     * @param index the index to insert value (negative such as -1, -n means the last one or n)
     * @param value the value to insert
     * @throws IndexOutOfBoundsException if the index is out of bounds(the origin values size)
     */
    default void insertValue(ItemStack item, int index, T value) {
        if (index == 0) {
            prependValue(item, value);
        } else if (index == -1) {
            appendValue(item, value);
        } else {
            ImmutableList<T> oldValues = this.getValues(item);
            final int size = oldValues.size();
            if (index == size) {
                this.setValues(item, ImmutableList.<T>builder().addAll(oldValues).add(value).build());
            } else {
                ImmutableList.Builder<T> builder = ImmutableList.builder();
                int splitIndex;
                if (index > 0) {
                    splitIndex = index - 1;
                    builder.addAll(oldValues.subList(0, splitIndex));
                    builder.add(value);
                    builder.addAll(oldValues.subList(splitIndex, oldValues.size()));
                    this.setValues(item, builder.build());
                } else {
                    //index < 0
                    splitIndex = oldValues.size() - index + 1;
                    builder.addAll(oldValues.subList(0, splitIndex));
                    builder.add(value);
                    builder.addAll(oldValues.subList(splitIndex, oldValues.size()));
                }
            }
        }
    }

    default void clearValues(ItemStack item) {
        this.setValues(item, ImmutableList.of());
    }

    @NonnullByDefault
    interface RegistryEvent extends Event {
        /**
         * @throws IllegalArgumentException if {@code id} is already registered
         */
        <T extends DataSerializable> Attribute<T> register(String id, Class<T> dataClass, AttributeToLoreFunction<T> toLoreFunction) throws IllegalArgumentException;

        default <T extends DataSerializable> Attribute<T> register(String id, Class<T> dataClass, byte priority, Function<? super T, ? extends Text> function) throws IllegalArgumentException {
            return this.register(id, dataClass, (values, equipable) -> values.stream().<Text>map(function).map(text -> Maps.immutableEntry(priority, text)).collect(ImmutableList.toImmutableList()));
        }
    }
}
