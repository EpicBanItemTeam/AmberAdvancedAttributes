package io.izzel.aaa.collector;

import io.izzel.aaa.service.Attribute;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.event.entity.TargetEntityEvent;
import org.spongepowered.api.event.entity.living.TargetLivingEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.util.List;
import java.util.Set;

/**
 * @author ustc_zzzz
 */
public interface AttributeCollectionEvent extends TargetEntityEvent {
    /**
     * @return target item
     */
    ItemStackSnapshot getTargetItem();

    /**
     * @return an unmodifiable set which contains attributes to be collected
     */
    Set<Attribute<? extends DataSerializable>> getCollectedAttributes();

    /**
     * The default elements are return elements of {@link Attribute#getValues}
     *
     * @return a modifiable list which can be used to add values or clear values
     */
    <T extends DataSerializable> List<? super T> getCollectionByAttribute(Attribute<T> attribute);
}
