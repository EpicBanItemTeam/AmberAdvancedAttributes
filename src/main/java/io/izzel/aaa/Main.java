package io.izzel.aaa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import io.izzel.aaa.data.DataUtil;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.Attributes;
import io.izzel.aaa.service.AttributeService;
import io.izzel.aaa.service.AttributeServiceImpl;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

@Plugin(id = "amberadvancedattributes")
public class Main {

    public static Main INSTANCE;

    public final Logger logger;
    public final AttributeServiceImpl service;

    private final Text loreSeparator = Text.of();

    @Inject
    public Main(Logger logger) {
        INSTANCE = this;
        this.logger = logger;
        this.service = new AttributeServiceImpl(this);
    }

    @Listener
    public void on(GameInitializationEvent event) {
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

    @Listener
    public void on(Attribute.RegistryEvent event) {
        event.register("aaa-attack", TypeToken.of(RangeValue.class), values -> ImmutableList.of()); // TODO
        event.register("aaa-possession", TypeToken.of(GameProfile.class), Byte.MIN_VALUE, (value) -> {
            GameProfile profile = Sponge.getServer().getGameProfileManager().fill(value).join();
            return Text.of(TextColors.YELLOW, profile.getName().orElse("[Server]"), " is possessed of this item");
        });
        CommandExecutor executor = (src, args) -> {
            if (src instanceof Player) {
                ItemStack item = ((Player) src).getItemInHand(HandTypes.MAIN_HAND).orElse(ItemStack.empty());
                Attributes.POSSESSION.setValues(item, ImmutableList.of(((Player) src).getProfile()));
                return CommandResult.success();
            } else {
                return CommandResult.empty();
            }
        };
        Sponge.getCommandManager().register(this, CommandSpec.builder().executor(executor).build(), "aaa-possess");
    }
}
