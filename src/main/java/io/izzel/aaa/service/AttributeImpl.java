package io.izzel.aaa.service;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;
import java.util.function.Function;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class AttributeImpl<T extends DataSerializable> implements Attribute<T> {
    private final Function<T, Text> toLoreFunction;
    private final TypeToken<T> token;
    private final String id;

    AttributeImpl(String id, TypeToken<T> token, Function<T, Text> toLoreFunction) {
        this.toLoreFunction = toLoreFunction;
        this.token = token;
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public TypeToken<T> getToken() {
        return this.token;
    }

    @Override
    public Text getLore(T value) {
        return this.toLoreFunction.apply(value);
    }

    @Override
    public ImmutableList<T> getValues(ItemStack item) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public void setValues(ItemStack item, List<? extends T> values) {
        throw new UnsupportedOperationException(); // TODO
    }
}
