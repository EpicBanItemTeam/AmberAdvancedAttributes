package io.izzel.aaa.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import io.izzel.aaa.service.Attribute;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

/**
 * @author ustc_zzzz
 */
public final class DataUtil {

    public static boolean hasData(ItemStack item) {
        return ImmutableList.of().size() > 0; // TODO: it is always false
    }

    public static <T extends DataSerializable> void collectLore(ListMultimap<Byte, Text> t, ItemStack i, Attribute<T> a) {
        t.putAll(a.getLoreTexts(a.getValues(i)));
    }

    private DataUtil() {
        throw new UnsupportedOperationException();
    }
}
