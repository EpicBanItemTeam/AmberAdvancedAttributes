package io.izzel.aaa.service;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
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

    Text getLore(T value);

    TypeToken<T> getToken();

    ImmutableList<T> getValues(ItemStack item);

    void setValues(ItemStack item, List<? extends T> values);

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
        <T extends DataSerializable> Attribute<T> register(String id, TypeToken<T> token, Function<T, Text> toLoreFunction) throws IllegalArgumentException;
    }
}
