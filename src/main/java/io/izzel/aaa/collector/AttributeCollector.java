package io.izzel.aaa.collector;

import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.AttributeService;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;

import java.util.List;

/**
 * @author ustc_zzzz
 */
public interface AttributeCollector {
    <T extends DataSerializable> AttributeCollector collect(Attribute<T> attribute, List<? super T> collection);

    boolean submit();

    static AttributeCollector of(ItemStackSnapshot stackSnapshot) {
        return AttributeService.instance().createCollector(stackSnapshot);
    }

    static AttributeCollector of(ItemStack stack) {
        return AttributeService.instance().createCollector(stack.createSnapshot());
    }
}
