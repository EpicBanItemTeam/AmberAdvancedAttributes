package io.izzel.aaa;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.izzel.aaa.data.DataUtil;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.AttributeService;
import io.izzel.aaa.service.AttributeServiceImpl;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

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
            ItemStack item = transaction.getFinal().createStack();
            if (DataUtil.hasData(item)) {
                Map<Byte, List<Text>> texts = new TreeMap<>();
                Map<String, Attribute<?>> attributes = AttributeService.instance().getAttributes();
                attributes.values().forEach(attribute -> DataUtil.collectLore(texts, item, attribute));
                item.offer(Keys.ITEM_LORE, texts.values().stream().reduce(ImmutableList.of(), (a, b) -> {
                    if (a.isEmpty()) {
                        return b;
                    } else {
                        return ImmutableList.<Text>builder().addAll(a).add(this.loreSeparator).addAll(b).build();
                    }
                }));
            }
        }
    }
}
