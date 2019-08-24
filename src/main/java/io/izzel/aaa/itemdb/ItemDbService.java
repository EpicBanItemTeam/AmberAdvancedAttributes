package io.izzel.aaa.itemdb;

import com.google.inject.ProvidedBy;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.Map;

@ProvidedBy(ItemDbServiceProvider.class)
public interface ItemDbService {

    ItemStack read(String id);

    void save(ItemStack stack, String id);

    Map<String, ItemStack> list();

    void delete(String id);

}
