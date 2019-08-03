package io.izzel.aaa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Inject;
import io.izzel.aaa.data.DataUtil;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.AttributeService;
import io.izzel.aaa.service.AttributeServiceImpl;
import org.slf4j.Logger;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

@Plugin(id = "amberadvancedattributes")
public class Main {
    public final Logger logger;
    public final AttributeServiceImpl service;

    private final Text loreSeparator = Text.of();

    @Inject
    public Main(Logger logger) {
        this.logger = logger;
        this.service = new AttributeServiceImpl(this);
    }

    @Listener
    public void on(GamePostInitializationEvent event) {
        this.service.init();
    }

    @Listener
    public void on(ChangeEntityEquipmentEvent event) {
        Transaction<ItemStackSnapshot> transaction = event.getTransaction();
        if (transaction.isValid()) {
            ListMultimap<Byte, Text> texts;
            ItemStack item = transaction.getFinal().createStack();
            if (DataUtil.hasData(item)) {
                texts = Multimaps.newListMultimap(new TreeMap<>(), ArrayList::new);
                Map<String, Attribute<?>> attributes = AttributeService.instance().getAttributes();
                attributes.values().forEach(attribute -> DataUtil.collectLore(texts, item, attribute));
                item.offer(Keys.ITEM_LORE, Multimaps.asMap(texts).values().stream().reduce(ImmutableList.of(), (a, b) -> {
                    if (a.isEmpty()) {
                        return b;
                    } else {
                        return ImmutableList.<Text>builder().addAll(a).add(this.loreSeparator).addAll(b).build();
                    }
                }));
                transaction.setCustom(item.createSnapshot());
            }
        }
    }
}
