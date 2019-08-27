package io.izzel.aaa.collector;

import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.AttributeServiceImpl;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ustc_zzzz
 */
public class AttributeCollectorImpl implements AttributeCollector {
    private final Map<Attribute<?>, List<?>> collections = new LinkedHashMap<>();
    private final AttributeServiceImpl service;
    private final ItemStackSnapshot snapshot;
    private final EquipmentType equipment;

    public AttributeCollectorImpl(AttributeServiceImpl service, ItemStackSnapshot snapshot, EquipmentType equipment) {
        this.service = service;
        this.snapshot = snapshot;
        this.equipment = equipment;
    }

    @Override
    public <T extends DataSerializable> AttributeCollector collect(Attribute<T> attribute, List<? super T> collection) {
        this.collections.put(attribute, collection);
        return null;
    }

    @Override
    public boolean submit() {
        throw new UnsupportedOperationException();
    }
}
