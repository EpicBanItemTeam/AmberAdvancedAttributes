package io.izzel.aaa.util;

import com.google.common.collect.ListMultimap;
import io.izzel.aaa.collector.AttributeCollector;
import io.izzel.aaa.data.Data;
import io.izzel.aaa.data.ImmutableData;
import io.izzel.aaa.service.Attribute;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author ustc_zzzz
 */
public final class DataUtil {

    private DataUtil() {
        throw new UnsupportedOperationException();
    }

    public static boolean hasData(ItemStack item) {
        return item.get(Data.class).isPresent();
    }

    public static Optional<Data> getData(ItemStack item) {
        return item.get(Data.class);
    }

    public static Optional<ImmutableData> getData(ItemStackSnapshot item) {
        return item.get(ImmutableData.class);
    }

    public static boolean setData(ItemStack item, Data data) {
        return item.offer(data).isSuccessful();
    }

    public static boolean dropData(ItemStack item) {
        return item.remove(Data.class).isSuccessful();
    }

    public static Data getOrCreateData(ItemStack item) {
        return item.getOrCreate(Data.class).orElseThrow(InvalidDataException::new);
    }

    public static <T extends DataSerializable> void collectLore(ListMultimap<Byte, Text> t, ItemStack i, Attribute<T> a, Equipable e) {
        t.putAll(a.getLoreTexts(a.getValues(i), e));
    }

    public static <T extends DataSerializable> void collectAllLore(ListMultimap<Byte, Text> t, ItemStackSnapshot i, Attribute<T> a, Equipable e) {
        List<T> collection = new ArrayList<>();
        Sponge.getCauseStackManager().pushCause(e);
        if (AttributeCollector.of(i).collect(a, collection).submit()) {
            t.putAll(a.getLoreTexts(collection, e));
        }
    }
}
