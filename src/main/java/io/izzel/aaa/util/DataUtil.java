package io.izzel.aaa.util;

import com.google.common.collect.ListMultimap;
import io.izzel.aaa.data.Data;
import io.izzel.aaa.service.Attribute;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

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

    public static boolean setData(ItemStack item, Data data) {
        return item.offer(data).isSuccessful();
    }

    public static boolean dropData(ItemStack item) {
        return item.remove(Data.class).isSuccessful();
    }

    public static Data getOrCreateData(ItemStack item) {
        return item.getOrCreate(Data.class).orElseThrow(InvalidDataException::new);
    }

    public static <T extends DataSerializable> void collectLore(ListMultimap<Byte, Text> t, ItemStack i, Attribute<T> a) {
        t.putAll(a.getLoreTexts(a.getValues(i)));
    }
}
