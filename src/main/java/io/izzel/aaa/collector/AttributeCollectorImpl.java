package io.izzel.aaa.collector;

import io.izzel.aaa.service.Attribute;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.*;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class AttributeCollectorImpl implements AttributeCollector {
    private final Map<Attribute<?>, List<?>> collections = new LinkedHashMap<>();
    private final ItemStackSnapshot snapshot;

    public AttributeCollectorImpl(ItemStackSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public <T extends DataSerializable> AttributeCollector collect(Attribute<T> attribute, List<? super T> collection) {
        this.collections.put(attribute, collection);
        return this;
    }

    @Override
    public boolean submit() {
        try (var frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            Sponge.getEventManager().post(new Event(frame.getCurrentCause(), this.snapshot, this.collections));
            return this.collections.values().stream().mapToInt(List::size).sum() > 0;
        }
    }

    private static class Event implements AttributeCollectionEvent {
        private final Map<Attribute<?>, List<?>> collections;
        private final Set<Attribute<?>> attributes;
        private final ItemStackSnapshot snapshot;
        private final Entity entity;
        private final Cause cause;

        @SuppressWarnings("unchecked")
        private Event(Cause cause, ItemStackSnapshot snapshot, Map<Attribute<?>, List<?>> collections) {
            this.attributes = Collections.unmodifiableSet(collections.keySet());
            for (var key : this.attributes) {
                List value = collections.get(key);
                if (value.isEmpty()) {
                    value.addAll(key.getValues(snapshot));
                }
            }
            this.entity = cause.first(Entity.class).orElseThrow(() -> new IllegalArgumentException("No entity present"));
            this.collections = collections;
            this.snapshot = snapshot;
            this.cause = cause;
        }

        @Override
        public ItemStackSnapshot getTargetItem() {
            return this.snapshot;
        }

        @Override
        public Set<Attribute<? extends DataSerializable>> getCollectedAttributes() {
            return this.attributes;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends DataSerializable> List<? super T> getCollectionByAttribute(Attribute<T> attribute) {
            if (!this.collections.containsKey(attribute)) {
                throw new IllegalArgumentException("Collection for " + attribute.getId() + " should be available");
            }
            return (List) this.collections.get(attribute);
        }

        @Override
        public Entity getTargetEntity() {
            return this.entity;
        }

        @Override
        public Cause getCause() {
            return this.cause;
        }
    }
}