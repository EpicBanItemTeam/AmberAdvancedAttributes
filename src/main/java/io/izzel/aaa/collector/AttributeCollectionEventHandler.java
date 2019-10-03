package io.izzel.aaa.collector;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.aaa.byteitems.ByteItemsHandler;
import io.izzel.aaa.data.InlayData;
import io.izzel.aaa.data.StringValue;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.Attributes;
import io.izzel.aaa.util.EquipmentUtil;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.*;

/**
 * @author ustc_zzzz
 */
@Singleton
@NonnullByDefault
public class AttributeCollectionEventHandler {
    private final ByteItemsHandler handler;
    private final Logger logger;

    @Inject
    public AttributeCollectionEventHandler(PluginContainer container, ByteItemsHandler handler, Logger logger, EventManager manager) {
        this.logger = logger;
        this.handler = handler;
        manager.registerListener(container, AttributeCollectionEvent.class, this::onSuit);
        manager.registerListener(container, AttributeCollectionEvent.class, this::onTemplate);
        manager.registerListener(container, AttributeCollectionEvent.class, this::onInlay);
        manager.registerListener(container, AttributeCollectionEvent.class, Order.LATE, this::onInlayDistinct);
    }

    private void onInlay(AttributeCollectionEvent event) {
        var snapshot = event.getTargetItem();
        List<InlayData> inlay = Attributes.INLAY.getValues(snapshot);
        inlay.stream().filter(it -> it.getGem().isPresent())
                .map(it -> it.getGem().get())
                .map(handler::read)
                .map(AttributeCollector::of)
                .forEach(collector -> {
                    for (var attribute : event.getCollectedAttributes()) {
                        if (!attribute.equals(Attributes.INLAY_GEM))
                            collector = this.collect(attribute, event, collector);
                    }
                    collector.submit();
                });
    }

    private void onInlayDistinct(AttributeCollectionEvent event) {
        if (event.getCollectedAttributes().contains(Attributes.INLAY)) {
            Map<String, InlayData> map = new LinkedHashMap<>();
            var list = event.getCollectionByAttribute(Attributes.INLAY);
            list.stream()
                    .map(InlayData.class::cast)
                    .forEach(data -> map.compute(data.getSlot(), (s, oldData) -> {
                        if (oldData == null || !oldData.getGem().isPresent()) return data;
                        else return oldData;
                    }));
            list.clear();
            list.addAll(map.values());
        }
    }

    private void onSuit(AttributeCollectionEvent event) {
        if (!event.getCause().contains(this.handler)) {
            if (event.getTargetEntity() instanceof Equipable) {
                var item = event.getTargetItem();
                var equipable = (Equipable) event.getTargetEntity();
                for (var value : Attributes.SUIT.getValues(item)) {
                    if (EquipmentUtil
                            .items(equipable).filter(i -> !i.isEmpty())
                            .map(Attributes.SUIT::getValues).allMatch(values -> values.contains(value))) {
                        Sponge.getCauseStackManager().pushCause(this.handler);
                        var collector = AttributeCollector.of(item);
                        for (var attribute : event.getCollectedAttributes()) {
                            collector = this.collect(attribute, event, collector);
                        }
                        collector.submit();
                    }
                }
            }
        }
    }

    private void onTemplate(AttributeCollectionEvent event) {
        Set<StringValue> set = new LinkedHashSet<>(Attributes.TEMPLATE.getValues(event.getTargetItem()).reverse());
        Deque<StringValue> deque = new ArrayDeque<>(set);
        while (!deque.isEmpty()) {
            var template = deque.removeLast().getString().replace(";", "");
            var templateItem = this.handler.read(template);
            if (!templateItem.isEmpty()) {
                for (var attribute : event.getCollectedAttributes()) {
                    this.merge(attribute, event, templateItem);
                }
                for (var value : Attributes.TEMPLATE.getValues(templateItem).reverse()) {
                    if (!set.contains(value)) {
                        deque.addLast(value);
                        set.add(value);
                        continue;
                    }
                    this.logger.warn("Duplicate template: {} (removed to prevent infinite loops)", value.getString());
                }
            }
        }
    }

    private <T extends DataSerializable> AttributeCollector collect(Attribute<T> attribute, AttributeCollectionEvent event, AttributeCollector collector) {
        return collector.collect(attribute, event.getCollectionByAttribute(attribute));
    }

    private <T extends DataSerializable> void merge(Attribute<T> attribute, AttributeCollectionEvent event, ItemStackSnapshot item) {
        event.getCollectionByAttribute(attribute).addAll(attribute.getValues(item));
    }
}
