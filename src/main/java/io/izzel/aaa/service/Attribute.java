package io.izzel.aaa.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Maps;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;
import java.util.function.Function;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public interface Attribute<T extends DataSerializable> {

    String getId();

    Class<T> getDataClass();

    ImmutableListMultimap<Byte, Text> getLoreTexts(List<? extends T> values);

    ImmutableList<T> getValues(ItemStack item);

    void setValues(ItemStack item, List<? extends T> values);

    default void prependValue(ItemStack item, T value) {
        this.setValues(item, ImmutableList.<T>builder().add(value).addAll(this.getValues(item)).build());
    }

    default void appendValue(ItemStack item, T value) {
        this.setValues(item, ImmutableList.<T>builder().addAll(this.getValues(item)).add(value).build());
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
            return this.register(id, dataClass, values -> values.stream().<Text>map(function).map(text -> Maps.immutableEntry(priority, text)).collect(ImmutableList.toImmutableList()));
        }
    }
}
