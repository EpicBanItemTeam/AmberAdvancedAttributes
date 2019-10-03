package io.izzel.aaa.listener;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import io.izzel.aaa.service.AttributeService;
import io.izzel.aaa.service.Attributes;
import io.izzel.aaa.template.LoreTemplateService;
import io.izzel.aaa.util.DataUtil;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * @author ustc_zzzz
 */
@Singleton
public class AttributeListeners {

    public static final Text LORE_SEPARATOR = Text.of();

    @Inject
    public AttributeListeners(PluginContainer container, EventManager eventManager, Injector injector) {
        eventManager.registerListeners(container, injector.getInstance(AttackListener.class));
        eventManager.registerListeners(container, injector.getInstance(ArrowListener.class));
        eventManager.registerListeners(container, injector.getInstance(PossessionListener.class));
        eventManager.registerListeners(container, injector.getInstance(MiscListener.class));
        eventManager.registerListeners(container, injector.getInstance(InlayListener.class));
        eventManager.registerListener(container, ChangeEntityEquipmentEvent.class, Order.LATE, this::on);
    }

    private void on(ChangeEntityEquipmentEvent event) {
        var transaction = event.getTransaction();
        if (transaction.isValid() && event.getTargetEntity() instanceof Equipable) {
            ListMultimap<Byte, Text> texts;
            var key = Keys.ITEM_LORE;
            var item = transaction.getFinal().createStack();
            if (DataUtil.hasData(item)) {
                var template = Attributes.LORE_TEMPLATE.getValues(item);
                if (!template.isEmpty()) {
                    var string = template.get(0).getString();
                    var list = LoreTemplateService.instance().eval(string, ((Equipable) event.getTargetEntity()), item);
                    item.offer(key, list);
                    transaction.setCustom(item.createSnapshot());
                } else if (Attributes.NO_LORE.getValues(item).isEmpty()) {
                    texts = Multimaps.newListMultimap(new TreeMap<>(), ArrayList::new);
                    var attributes = AttributeService.instance().getAttributes();
                    attributes.values().forEach(attribute -> DataUtil.collectLore(texts, item, attribute, (Equipable) event.getTargetEntity()));
                    item.offer(key, Multimaps.asMap(texts).values().stream().reduce(ImmutableList.of(), (a, b) -> {
                        if (a.isEmpty()) {
                            return b;
                        } else {
                            return ImmutableList.<Text>builder().addAll(a).add(LORE_SEPARATOR).addAll(b).build();
                        }
                    }));
                    transaction.setCustom(item.createSnapshot());
                }
            }
        }
    }

}