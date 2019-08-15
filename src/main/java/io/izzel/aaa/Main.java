package io.izzel.aaa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.izzel.aaa.util.DataUtil;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.listener.ArrowListener;
import io.izzel.aaa.listener.AttackListener;
import io.izzel.aaa.listener.MiscListener;
import io.izzel.aaa.listener.PossessionListener;
import io.izzel.aaa.service.*;
import io.izzel.amber.commons.i18n.AmberLocale;
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
import java.util.TreeMap;

import static io.izzel.aaa.service.AttributeToLoreFunctions.*;

@Plugin(id = "amberadvancedattributes", description = "An AmberAdvancedAttributes item attribute plugin.")
public class Main {
    public final Logger logger;
    public final AmberLocale locale;
    public final AttributeServiceImpl service;

    private final Injector injector;

    private final Text loreSeparator = Text.of();

    @Inject
    public Main(Logger logger, AttributeServiceImpl service, AmberLocale locale, Injector injector) {
        this.logger = logger;
        this.locale = locale;
        this.service = service;
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
        TypeToken<RangeValue> rangeType = TypeToken.of(RangeValue.class);
        TypeToken<GameProfile> profileType = TypeToken.of(GameProfile.class);
        TypeToken<RangeValue.Fixed> rangeFixedType = TypeToken.of(RangeValue.Fixed.class);

        event.register("aaa-attack", rangeType, rangeValue(this, "attack"));
        event.register("aaa-tracing", rangeType, rangeValue(this, "tracing"));
        event.register("aaa-possession", profileType, profile(this));
        event.register("aaa-pvp-attack", rangeType, rangeValue(this, "pvp-attack"));
        event.register("aaa-pve-attack", rangeType, rangeValue(this, "pve-attack"));
        event.register("aaa-defense", rangeType, rangeValue(this, "defense"));
        event.register("aaa-pvp-defense", rangeType, rangeValue(this, "pvp-defense"));
        event.register("aaa-pve-defense", rangeType, rangeValue(this, "pve-defense"));
        event.register("aaa-reflect", rangeType, rangeValue(this, "reflect"));
        event.register("aaa-pvp-reflect", rangeType, rangeValue(this, "pvp-reflect"));
        event.register("aaa-pve-reflect", rangeType, rangeValue(this, "pve-reflect"));
        event.register("aaa-reflect-rate", rangeType, rangeValue(this, "reflect-rate"));
        event.register("aaa-critical", rangeType, rangeValue(this, "critical"));
        event.register("aaa-critical-rate", rangeType, rangeValue(this, "critical-rate"));
        event.register("aaa-dodge", rangeType, rangeValue(this, "dodge"));
        event.register("aaa-accuracy", rangeType, rangeValue(this, "accuracy"));
        event.register("aaa-accelerate", rangeType, rangeValue(this, "accelerate"));
        event.register("aaa-attack-speed", rangeFixedType, rangeValue(this, "attack-speed"));
        event.register("aaa-move-speed", rangeFixedType, rangeValue(this, "move-speed"));
        event.register("aaa-durability", rangeType, durability(this));
        event.register("aaa-unbreakable", rangeFixedType, markerValue(this, "unbreakable"));
        event.register("aaa-loot-rate", rangeType, rangeValue(this, "loot-rate"));
        event.register("aaa-loot-immune", rangeType, markerValue(this, "loot-immune"));
        event.register("aaa-burn", rangeType, rangeValue(this, "burn"));
        event.register("aaa-burn-rate", rangeType, rangeValue(this, "burn-rate"));
        event.register("aaa-life-steal", rangeType, rangeValue(this, "life-steal"));
        event.register("aaa-life-steal-rate", rangeType, rangeValue(this, "life-steal-rate"));
        event.register("aaa-max-health", rangeFixedType, rangeValue(this, "max-health"));
        event.register("aaa-attack-range", rangeFixedType, rangeValue(this, "attack-range"));
        event.register("aaa-starvation", rangeType, rangeValue(this, "starvation"));
        event.register("aaa-saturation", rangeType, rangeValue(this, "saturation"));
        event.register("aaa-regeneration", rangeType, rangeValue(this, "regeneration"));
        event.register("aaa-knockback", rangeType, rangeValue(this, "knockback"));
        event.register("aaa-instant-death", rangeType, rangeValue(this, "instant-death"));
        event.register("aaa-instant-death-immune", rangeFixedType, markerValue(this, "instant-death-immune"));

        EventManager eventManager = Sponge.getEventManager();

        eventManager.registerListeners(this, this.injector.getInstance(AttackListener.class));
        eventManager.registerListeners(this, this.injector.getInstance(ArrowListener.class));
        eventManager.registerListeners(this, this.injector.getInstance(PossessionListener.class));
        eventManager.registerListeners(this, this.injector.getInstance(MiscListener.class));

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
