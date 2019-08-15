package io.izzel.aaa.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.reflect.TypeToken;
import io.izzel.aaa.data.DataUtil;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.item.inventory.ItemStack;
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
    private final TypeToken<T> token;
    private final String id;

    AttributeImpl(String id, TypeToken<T> token, AttributeToLoreFunction<T> toLoreFunction) {
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
    public ImmutableListMultimap<Byte, Text> getLoreTexts(List<? extends T> values) {
        Stream<Map.Entry<Byte, Text>> stream = this.toLoreFunction.toLoreTexts(values).stream();
        return stream.collect(ImmutableListMultimap.toImmutableListMultimap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public ImmutableList<T> getValues(ItemStack item) {
        return DataUtil.getData(item).map(data -> ImmutableList.<T>copyOf(data.get(this))).orElse(ImmutableList.of());
    }

    @Override
    public void setValues(ItemStack item, List<? extends T> values) {
        DataUtil.setData(item, DataUtil.getOrCreateData(item).set(this, values));
    }
}
