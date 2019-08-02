package io.izzel.aaa.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimaps;
import io.izzel.aaa.service.Attribute;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Map;

/**
 * @author ustc_zzzz
 */
public final class DataUtil {

    public static boolean hasData(ItemStack item) {
        return ImmutableList.of().size() > 0; // TODO: it is always false
    }

    public static <T extends DataSerializable> void collectLore(Map<Byte, List<Text>> t, ItemStack i, Attribute<T> a) {
        t.putAll(Multimaps.asMap(a.getLoreTexts(a.getValues(i))));
    }

    private DataUtil() {
        throw new UnsupportedOperationException();
    }
}
