package io.izzel.aaa.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import io.izzel.aaa.util.DataUtil;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class AttributeImpl<T extends DataSerializable> implements Attribute<T> {
    private final AttributeToLoreFunction<T> toLoreFunction;
    private final Class<T> dataClass;
    private final String id;

    AttributeImpl(String id, Class<T> dataClass, AttributeToLoreFunction<T> toLoreFunction) {
        this.toLoreFunction = toLoreFunction;
        this.dataClass = dataClass;
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Class<T> getDataClass() {
        return this.dataClass;
    }

    @Override
    public ImmutableListMultimap<Byte, Text> getLoreTexts(List<? extends T> values, Equipable equipable) {
        Stream<Map.Entry<Byte, Text>> stream = this.toLoreFunction.toLoreTexts(values, equipable).stream();
        return stream.collect(ImmutableListMultimap.toImmutableListMultimap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public ImmutableList<T> getValues(ItemStack item) {
        return DataUtil.getData(item).map(data -> data.get(this)).orElse(ImmutableList.of());
    }

    @Override
    public ImmutableList<T> getValues(ItemStackSnapshot item) {
        return DataUtil.getData(item).map(data -> data.get(this)).orElse(ImmutableList.of());
    }

    @Override
    public void setValues(ItemStack item, List<? extends T> values) {
        DataUtil.setData(item, DataUtil.getOrCreateData(item).set(this, values));
    }
}
