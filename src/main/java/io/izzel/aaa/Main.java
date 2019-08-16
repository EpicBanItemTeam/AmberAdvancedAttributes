package io.izzel.aaa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.izzel.aaa.command.RangeValueElement;
import io.izzel.aaa.data.MarkerValue;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.listener.ArrowListener;
import io.izzel.aaa.listener.AttackListener;
import io.izzel.aaa.listener.MiscListener;
import io.izzel.aaa.listener.PossessionListener;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.AttributeService;
import io.izzel.aaa.service.AttributeServiceImpl;
import io.izzel.aaa.service.AttributeToLoreFunction;
import io.izzel.aaa.util.DataUtil;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.amber.commons.i18n.args.Arg;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

    @Listener
    public void on(Attribute.RegistryEvent event) {
        CommandManager commandManager = Sponge.getCommandManager();

        this.registerPossessValue(event, "possession", commandManager);
        this.registerRangeValue(event, "attack", commandManager);
        this.registerRangeValue(event, "tracing", commandManager);
        this.registerRangeValue(event, "pvp-attack", commandManager);
        this.registerRangeValue(event, "pve-attack", commandManager);
        this.registerRangeValue(event, "defense", commandManager);
        this.registerRangeValue(event, "pvp-defense", commandManager);
        this.registerRangeValue(event, "pve-defense", commandManager);
        this.registerRangeValue(event, "reflect", commandManager);
        this.registerRangeValue(event, "pvp-reflect", commandManager);
        this.registerRangeValue(event, "pve-reflect", commandManager);
        this.registerRangeValue(event, "reflect-rate", commandManager);
        this.registerRangeValue(event, "critical", commandManager);
        this.registerRangeValue(event, "critical-rate", commandManager);
        this.registerRangeValue(event, "dodge", commandManager);
        this.registerRangeValue(event, "accuracy", commandManager);
        this.registerRangeValue(event, "accelerate", commandManager);
        this.registerRangeValueFixed(event, "attack-speed", commandManager);
        this.registerRangeValueFixed(event, "move-speed", commandManager);
        this.registerDurabilityValue(event, "durability", commandManager);
        this.registerMarkerValue(event, "unbreakable", commandManager);
        this.registerRangeValue(event, "loot-rate", commandManager);
        this.registerMarkerValue(event, "loot-immune", commandManager);
        this.registerRangeValue(event, "burn", commandManager);
        this.registerRangeValue(event, "burn-rate", commandManager);
        this.registerRangeValue(event, "life-steal", commandManager);
        this.registerRangeValue(event, "life-steal-rate", commandManager);
        this.registerRangeValueFixed(event, "max-health", commandManager);
        this.registerRangeValueFixed(event, "attack-range", commandManager);
        this.registerRangeValue(event, "starvation", commandManager);
        this.registerRangeValue(event, "saturation", commandManager);
        this.registerRangeValue(event, "regeneration", commandManager);
        this.registerRangeValue(event, "knockback", commandManager);
        this.registerRangeValue(event, "instant-death", commandManager);
        this.registerMarkerValue(event, "instant-death-immune", commandManager);
        this.registerTextValue(event, "original-lore", commandManager);

        EventManager eventManager = Sponge.getEventManager();

        eventManager.registerListeners(this, this.injector.getInstance(AttackListener.class));
        eventManager.registerListeners(this, this.injector.getInstance(ArrowListener.class));
        eventManager.registerListeners(this, this.injector.getInstance(PossessionListener.class));
        eventManager.registerListeners(this, this.injector.getInstance(MiscListener.class));
    }

    private void registerTextValue(Attribute.RegistryEvent event, String id, CommandManager manager) {
        Attribute<Text> attribute = event.register("aaa-" + id, TypeToken.of(Text.class), values -> ImmutableList.of());
        manager.register(this, CommandSpec.builder()
                .permission("amberadvancedattributes.command.aaa-init")
                        .executor((src, args) -> {
                            if (src instanceof Player) {
                                Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                                if (stackOptional.isPresent()) {
                                    ItemStack stack = stackOptional.get();
                                    if (DataUtil.hasData(stack)) {
                                        this.locale.to(src, "commands.already-initialized");
                                        return CommandResult.success();
                                    } else {
                                        attribute.setValues(stack, stack.get(Keys.ITEM_LORE).orElse(ImmutableList.of()));
                                        ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                        this.locale.to(src, "commands.init-succeed");
                                        return CommandResult.success();
                                    }
                                }
                            }
                            this.locale.to(src, "commands.nonexist-attribute");
                            return CommandResult.success();
                        })
                        .build(), "aaa-init");
        manager.register(this, CommandSpec.builder()
                .permission("amberadvancedattributes.command.aaa-drop")
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        AtomicBoolean isCallbackExecuted = new AtomicBoolean(false);
                        locale.to(src, "commands.drop-warning", Arg.ref("commands.drop-warning-ok").withCallback(value -> {
                            if (!isCallbackExecuted.getAndSet(true)) {
                                Optional<ItemStack> stackOptional = ((Player) value).getItemInHand(HandTypes.MAIN_HAND);
                                if (stackOptional.isPresent()) {
                                    ItemStack stack = stackOptional.get();
                                    if (DataUtil.hasData(stack)) {
                                        List<Text> lore = attribute.getValues(stack);
                                        DataUtil.dropData(stack);
                                        stack.offer(Keys.ITEM_LORE, lore);
                                        ((Player) value).setItemInHand(HandTypes.MAIN_HAND, stack);
                                        this.locale.to(value, "commands.drop-succeed");
                                        return;
                                    }
                                }
                                this.locale.to(value, "commands.nonexist-attribute");
                            }
                        }));
                        return CommandResult.success();
                    }
                    this.locale.to(src, "commands.nonexist-attribute");
                    return CommandResult.success();
                })
                .build(), "aaa-drop");
    }

    private void registerDurabilityValue(Attribute.RegistryEvent event, String id, CommandManager manager) {
        this.registerRangeValue(event, durability(this), id, TypeToken.of(RangeValue.class), manager);
    }

    private void registerRangeValue(Attribute.RegistryEvent event, String id, CommandManager manager) {
        this.registerRangeValue(event, rangeValue(this, id), id, TypeToken.of(RangeValue.class), manager);
    }

    private void registerRangeValueFixed(Attribute.RegistryEvent event, String id, CommandManager manager) {
        this.registerRangeValue(event, rangeValue(this, id), id, TypeToken.of(RangeValue.Fixed.class), manager);
    }

    private <T extends RangeValue> void registerRangeValue(Attribute.RegistryEvent event, AttributeToLoreFunction<T> f,
                                                           String id, TypeToken<T> typeToken, CommandManager manager) {
        boolean fixed = RangeValue.Fixed.class.isAssignableFrom(typeToken.getRawType());
        Attribute<T> attribute = event.register("aaa-" + id, typeToken, f);
        manager.register(this, CommandSpec.builder()
                .permission("amberadvancedattributes.command.aaa-" + id)
                .child(CommandSpec.builder()
                        .executor((src, args) -> {
                            if (src instanceof Player) {
                                Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                                if (stackOptional.isPresent()) {
                                    ItemStack stack = stackOptional.get();
                                    if (DataUtil.hasData(stack)) {
                                        attribute.clearValues(stack);
                                        ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                        this.locale.to(src, "commands.clear-attribute", stack, id);
                                        return CommandResult.success();
                                    }
                                }
                            }
                            this.locale.to(src, "commands.nonexist-attribute");
                            return CommandResult.success();
                        })
                        .build(), "clear")
                .child(CommandSpec.builder()
                        .arguments(new RangeValueElement(this.locale, fixed, Text.of("value")))
                        .executor((src, args) -> {
                            if (src instanceof Player) {
                                Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                                Optional<T> rangeValueOptional = args.getOne(Text.of("value"));
                                if (stackOptional.isPresent() && rangeValueOptional.isPresent()) {
                                    ItemStack stack = stackOptional.get();
                                    if (DataUtil.hasData(stack)) {
                                        attribute.appendValue(stack, rangeValueOptional.get());
                                        ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                        this.locale.to(src, "commands.append-attribute", stack, id);
                                        return CommandResult.success();
                                    }
                                }
                            }
                            this.locale.to(src, "commands.nonexist-attribute");
                            return CommandResult.success();
                        })
                        .build(), "append")
                .child(CommandSpec.builder()
                        .arguments(new RangeValueElement(this.locale, fixed, Text.of("value")))
                        .executor((src, args) -> {
                            if (src instanceof Player) {
                                Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                                Optional<T> rangeValueOptional = args.getOne(Text.of("value"));
                                if (stackOptional.isPresent() && rangeValueOptional.isPresent()) {
                                    ItemStack stack = stackOptional.get();
                                    if (DataUtil.hasData(stack)) {
                                        attribute.prependValue(stack, rangeValueOptional.get());
                                        ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                        this.locale.to(src, "commands.prepend-attribute", stack, id);
                                        return CommandResult.success();
                                    }
                                }
                            }
                            this.locale.to(src, "commands.nonexist-attribute");
                            return CommandResult.success();
                        })
                        .build(), "prepend")
                .build(), "aaa-" + id);
    }

    private void registerMarkerValue(Attribute.RegistryEvent event, String id, CommandManager manager) {
        TypeToken<MarkerValue> token = TypeToken.of(MarkerValue.class);
        Attribute<MarkerValue> attribute = event.register("aaa-" + id, token, markerValue(this, id));
        manager.register(this, CommandSpec.builder()
                .permission("amberadvancedattributes.command.aaa-" + id)
                .arguments(GenericArguments.choices(Text.of("marked"), ImmutableMap.of("mark", Boolean.TRUE, "unmark", Boolean.FALSE)))
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Optional<Boolean> marked = args.getOne(Text.of("marked"));
                        if (stackOptional.isPresent() && marked.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                if (marked.get()) {
                                    attribute.setValues(stack, ImmutableList.of(MarkerValue.of()));
                                    ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                    this.locale.to(src, "commands.mark-attribute", stack, id);
                                    return CommandResult.success();
                                } else {
                                    attribute.clearValues(stack);
                                    ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                    this.locale.to(src, "commands.unmark-attribute", stack, id);
                                    return CommandResult.success();
                                }
                            }
                        }
                    }
                    this.locale.to(src, "commands.nonexist-attribute");
                    return CommandResult.success();
                })
                .build(), "aaa-" + id);
    }

    private void registerPossessValue(Attribute.RegistryEvent event, String id, CommandManager manager) {
        TypeToken<GameProfile> token = TypeToken.of(GameProfile.class);
        Attribute<GameProfile> attribute = event.register("aaa-" + id, token, profile(this));
        manager.register(this, CommandSpec.builder()
                .permission("amberadvancedattributes.command.aaa-possess")
                .arguments(GenericArguments.optional(GenericArguments.player(Text.of("player"))))
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Player target = args.<Player>getOne(Text.of("player")).orElse((Player) src);
                        if (stackOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.setValues(stack, ImmutableList.of(target.getProfile()));
                                this.locale.to(src, "commands.mark-possession-attribute", target.getName());
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.nonexist-attribute");
                    return CommandResult.success();
                })
                .build(), "aaa-possess");
        manager.register(this, CommandSpec.builder()
                .permission("amberadvancedattributes.command.aaa-publicize")
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        if (stackOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.clearValues(stack);
                                this.locale.to(src, "commands.unmark-possession-attribute");
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.nonexist-attribute");
                    return CommandResult.success();
                })
                .build(), "aaa-publicize");
    }
}
