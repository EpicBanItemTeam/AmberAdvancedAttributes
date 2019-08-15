package io.izzel.aaa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.izzel.aaa.data.DataUtil;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.listener.AttackListener;
import io.izzel.aaa.listener.MiscListener;
import io.izzel.aaa.listener.PossessionListener;
import io.izzel.aaa.listener.ArrowListener;
import io.izzel.aaa.service.*;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.amber.commons.i18n.args.Arg;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Plugin(id = "amberadvancedattributes", description = "An AmberAdvancedAttributes item attribute plugin.")
public class Main {

    public static Main INSTANCE;

    public final Logger logger;
    public final AttributeServiceImpl service;
    public final AmberLocale locale;
    private final Injector injector;

    private final Text loreSeparator = Text.of();

    @Inject
    public Main(Logger logger, AmberLocale locale, Injector injector) {
        INSTANCE = this;
        this.logger = logger;
        this.service = new AttributeServiceImpl(this);
        this.locale = locale;
        this.injector = injector;
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

    @SuppressWarnings("unchecked")
    @Listener
    public void on(Attribute.RegistryEvent event) {
        EventManager eventManager = Sponge.getEventManager();
        event.register("aaa-attack", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("attack"));
        eventManager.registerListeners(this, injector.getInstance(AttackListener.class));
        event.register("aaa-tracing", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("tracing"));
        eventManager.registerListeners(this, injector.getInstance(ArrowListener.class));
        event.register("aaa-possession", TypeToken.of(GameProfile.class), AttributeToLoreFunctions.profile());
        eventManager.registerListeners(this, injector.getInstance(PossessionListener.class));
        event.register("aaa-pvp-attack", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("pvp-attack"));
        event.register("aaa-pve-attack", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("pve-attack"));
        event.register("aaa-defense", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("defense"));
        event.register("aaa-pvp-defense", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("pvp-defense"));
        event.register("aaa-pve-defense", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("pve-defense"));
        event.register("aaa-reflect", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("reflect"));
        event.register("aaa-pvp-reflect", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("pvp-reflect"));
        event.register("aaa-pve-reflect", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("pve-reflect"));
        event.register("aaa-reflect-rate", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("reflect-rate"));
        event.register("aaa-critical", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("critical"));
        event.register("aaa-critical-rate", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("critical-rate"));
        event.register("aaa-dodge", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("dodge"));
        event.register("aaa-accuracy", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("accuracy"));
        event.register("aaa-accelerate", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("accelerate"));
        event.register("aaa-attack-speed", TypeToken.of(RangeValue.Fixed.class), AttributeToLoreFunctions.rangeValue("attack-speed"));
        event.register("aaa-move-speed", TypeToken.of(RangeValue.Fixed.class), AttributeToLoreFunctions.rangeValue("move-speed"));
        event.register("aaa-durability", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.durability());
        event.register("aaa-unbreakable", TypeToken.of(RangeValue.Fixed.class), AttributeToLoreFunctions.markerValue("unbreakable"));
        event.register("aaa-loot-rate", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("loot-rate"));
        event.register("aaa-loot-immune", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.markerValue("loot-immune"));
        event.register("aaa-burn", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("burn"));
        event.register("aaa-burn-rate", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("burn-rate"));
        event.register("aaa-life-steal", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("life-steal"));
        event.register("aaa-life-steal-rate", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("life-steal-rate"));
        event.register("aaa-max-health", TypeToken.of(RangeValue.Fixed.class), AttributeToLoreFunctions.rangeValue("max-health"));
        event.register("aaa-attack-range", TypeToken.of(RangeValue.Fixed.class), AttributeToLoreFunctions.rangeValue("attack-range"));
        event.register("aaa-starvation", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("starvation"));
        event.register("aaa-saturation", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("saturation"));
        event.register("aaa-regeneration", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("regeneration"));
        event.register("aaa-knockback", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("knockback"));
        event.register("aaa-instant-death", TypeToken.of(RangeValue.class), AttributeToLoreFunctions.rangeValue("instant-death"));
        event.register("aaa-instant-death-immune", TypeToken.of(RangeValue.Fixed.class), AttributeToLoreFunctions.markerValue("instant-death-immune"));
        eventManager.registerListeners(this, injector.getInstance(MiscListener.class));
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
        // todo 暂时用这个
        Sponge.getCommandManager().register(this, CommandSpec.builder()
            .child(CommandSpec.builder()
                .arguments(GenericArguments.string(Text.of("attr")), GenericArguments.integer(Text.of("value")))
                .executor((src, args) -> {
                    try {
                        Integer value = args.<Integer>getOne("value").get();
                        Field attr = Attributes.class.getDeclaredField(args.<String>getOne("attr").orElse(""));
                        attr.setAccessible(true);
                        Attribute attribute = (Attribute) attr.get(null);
                        ItemStack itemStack = ((Player) src).getItemInHand(HandTypes.MAIN_HAND).get();
                        attribute.setValues(itemStack, ImmutableList.of(RangeValue.absolute(value)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return CommandResult.success();
                })
                .build(), "add")
            .build(), "aaa");
    }
}
