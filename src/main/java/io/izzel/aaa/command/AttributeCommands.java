package io.izzel.aaa.command;

import com.google.common.collect.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.aaa.AmberAdvancedAttributes;
import io.izzel.aaa.byteitems.ByteItemsHandler;
import io.izzel.aaa.data.InlayData;
import io.izzel.aaa.data.MarkerValue;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.data.StringValue;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.AttributeService;
import io.izzel.aaa.service.AttributeToLoreFunction;
import io.izzel.aaa.service.Attributes;
import io.izzel.aaa.util.DataUtil;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.amber.commons.i18n.args.Arg;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.izzel.aaa.service.AttributeToLoreFunctions.*;

/**
 * @author ustc_zzzz
 */
@SuppressWarnings("SameParameterValue")
@Singleton
public class AttributeCommands {
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9]+([-_][a-z0-9]+)*");
    public static final Text LORE_SEPARATOR = Text.of();

    private final ByteItemsHandler biHandler;
    private final PluginContainer container;
    private final CommandManager commandManager;
    private final AmberLocale locale;

    @Inject
    public AttributeCommands(PluginContainer container, ByteItemsHandler biHandler, CommandManager c, EventManager eventManager, AmberLocale locale) {
        this.container = container;
        this.biHandler = biHandler;
        this.commandManager = c;
        this.locale = locale;
        eventManager.registerListener(container, Attribute.RegistryEvent.class, Order.EARLY, this::on);
        eventManager.registerListener(container, ChangeEntityEquipmentEvent.class, Order.LATE, this::on);
    }

    private void on(ChangeEntityEquipmentEvent event) {
        Transaction<ItemStackSnapshot> transaction = event.getTransaction();
        if (transaction.isValid() && event.getTargetEntity() instanceof Equipable) {
            ListMultimap<Byte, Text> texts;
            Key<ListValue<Text>> key = Keys.ITEM_LORE;
            ItemStack item = transaction.getFinal().createStack();
            if (DataUtil.hasData(item)) {
                texts = Multimaps.newListMultimap(new TreeMap<>(), ArrayList::new);
                Map<String, Attribute<?>> attributes = AttributeService.instance().getAttributes();
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

    private void on(Attribute.RegistryEvent event) {
        this.registerRangeValue(this.container, event, "attack");
        this.registerRangeValue(this.container, event, "tracing");
        this.registerRangeValue(this.container, event, "pvp-attack");
        this.registerRangeValue(this.container, event, "pve-attack");
        this.registerRangeValue(this.container, event, "defense");
        this.registerRangeValue(this.container, event, "pvp-defense");
        this.registerRangeValue(this.container, event, "pve-defense");
        this.registerRangeValue(this.container, event, "reflect");
        this.registerRangeValue(this.container, event, "pvp-reflect");
        this.registerRangeValue(this.container, event, "pve-reflect");
        this.registerRangeValue(this.container, event, "reflect-rate");
        this.registerRangeValue(this.container, event, "critical");
        this.registerRangeValue(this.container, event, "critical-rate");
        this.registerRangeValue(this.container, event, "dodge");
        this.registerRangeValue(this.container, event, "accuracy");
        this.registerRangeValue(this.container, event, "accelerate");
        this.registerRangeValueFixed(this.container, event, "attack-speed");
        this.registerRangeValueFixed(this.container, event, "move-speed");
        this.registerDurabilityValue(this.container, event, "durability");
        this.registerMarkerValue(this.container, event, "unbreakable");
        this.registerRangeValue(this.container, event, "loot-rate");
        this.registerMarkerValue(this.container, event, "loot-immune");
        this.registerRangeValue(this.container, event, "burn");
        this.registerRangeValue(this.container, event, "burn-rate");
        this.registerRangeValue(this.container, event, "life-steal");
        this.registerRangeValue(this.container, event, "life-steal-rate");
        this.registerRangeValueFixed(this.container, event, "max-health");
        this.registerRangeValueFixed(this.container, event, "attack-range");
        this.registerRangeValue(this.container, event, "starvation");
        this.registerRangeValue(this.container, event, "saturation");
        this.registerRangeValue(this.container, event, "regeneration");
        this.registerRangeValue(this.container, event, "knockback");
        this.registerRangeValue(this.container, event, "instant-death");
        this.registerMarkerValue(this.container, event, "instant-death-immune");
        this.registerPossessValue(this.container, event, "possession");
        this.registerTextValue(this.container, event, "original-lore");
        this.registerEquipment(this.container, event);
        this.registerSuit(this.container, event, "suit");
        this.registerTemplate(this.container, event, "template");
        this.registerCustomTextValue(this.container, event, "custom-lore");
        this.registerInlay(this.container, event, "inlay");
        this.registerMarkerValue(this.container, event, "inlay-gem");
        this.registerRangeValueFixed(this.container, event, "inlay-success");
        this.registerPermissionCap(this.container, event, "permission-cap");
        this.registerRangeValue(this.container, event, "level-cap");
        this.registerItemsCommand(this.container);

        event.register("aaa-id", StringValue.class, (v, e) -> ImmutableList.of());
    }

    private void registerItemsCommand(PluginContainer container) {
        this.commandManager.register(container, this.getItemsCommand(), "aaa-items");
    }

    private void registerPermissionCap(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<StringValue> function = permissionCap(this.locale);
        Attribute<StringValue> attribute = event.register("aaa-" + id, StringValue.class, function);
        this.commandManager.register(container, getStringCommand(attribute, id), "aaa-" + id);
    }

    private void registerInlay(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<InlayData> function = inlay(this.locale, this.biHandler);
        Attribute<InlayData> attribute = event.register("aaa-" + id, InlayData.class, function);
        InlayDataElement element = new InlayDataElement(id);
        this.commandManager.register(container, CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-" + id)
                .child(getAppendCommand(id, attribute, element), "append")
                .child(getPrependCommand(id, attribute, element), "prepend")
                .child(getClearCommand(id, attribute), "clear")
                .child(getInsertCommand(id, attribute, element), "insert")
                .build(), "aaa-" + id);
    }

    private void registerCustomTextValue(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<Text> function = (values, equipable) -> values.stream()
                .map(text -> Maps.immutableEntry((byte) 0, (Text) text))
                .collect(Collectors.toList());
        Attribute<Text> attribute = event.register("aaa-" + id, Text.class, function);
        this.commandManager.register(container, CommandSpec.builder().permission(AmberAdvancedAttributes.ID + ".command.aaa-" + id)
                .child(getAppendCommand(id, attribute, GenericArguments.text(Text.of("lore"), TextSerializers.FORMATTING_CODE, true)), "append")
                .child(getInsertCommand(id, attribute, GenericArguments.text(Text.of("lore"), TextSerializers.FORMATTING_CODE, true)), "insert")
                .child(getPrependCommand(id, attribute, GenericArguments.text(Text.of("lore"), TextSerializers.FORMATTING_CODE, true)), "prepend")
                .build(), "aaa-" + id);
    }

    private void registerTemplate(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<StringValue> function = template(this.locale, this.biHandler);
        Attribute<StringValue> attribute = event.register("aaa-" + id, StringValue.class, function);
        this.commandManager.register(container, getTemplateCommand(attribute, id), "aaa-" + id);
    }

    private void registerSuit(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<StringValue> function = suit(this.locale, this.biHandler);
        Attribute<StringValue> attribute = event.register("aaa-" + id, StringValue.class, function);
        this.commandManager.register(container, getStringCommand(attribute, id), "aaa-" + id);
    }

    private void registerTextValue(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<Text> function = (values, equipable) -> ImmutableList.of();
        Attribute<Text> attribute = event.register("aaa-" + id, Text.class, function);
        this.commandManager.register(container, this.getInitCommand(attribute), "aaa-init");
        this.commandManager.register(container, this.getDropCommand(attribute), "aaa-drop");
    }

    private void registerDurabilityValue(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<RangeValue> function = durability(this.locale);
        Attribute<RangeValue> attribute = event.register("aaa-" + id, RangeValue.class, function);
        this.commandManager.register(container, this.getRangeCommand(id, false, attribute), "aaa-" + id);
    }

    private void registerRangeValue(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<RangeValue> function = rangeValue(this.locale, id);
        Attribute<RangeValue> attribute = event.register("aaa-" + id, RangeValue.class, function);
        this.commandManager.register(container, this.getRangeCommand(id, false, attribute), "aaa-" + id);
    }

    private void registerRangeValueFixed(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<RangeValue.Fixed> function = rangeValue(this.locale, id);
        Attribute<RangeValue.Fixed> attribute = event.register("aaa-" + id, RangeValue.Fixed.class, function);
        this.commandManager.register(container, this.getRangeCommand(id, true, attribute), "aaa-" + id);
    }

    private void registerMarkerValue(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<MarkerValue> function = markerValue(this.locale, id);
        Attribute<MarkerValue> attribute = event.register("aaa-" + id, MarkerValue.class, function);
        this.commandManager.register(container, this.getMarkerCommand(id, attribute), "aaa-" + id);
    }

    private void registerPossessValue(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<GameProfile> function = profile(this.locale);
        Attribute<GameProfile> attribute = event.register("aaa-" + id, GameProfile.class, function);
        this.commandManager.register(container, this.getPossessCommand(attribute), "aaa-possess");
        this.commandManager.register(container, this.getPublicizeCommand(attribute), "aaa-publicize");
    }

    private void registerEquipment(PluginContainer container, Attribute.RegistryEvent event) {
        AttributeToLoreFunction<StringValue> function = equipment(this.locale);
        Attribute<StringValue> attribute = event.register("aaa-equipment", StringValue.class, function);
        this.commandManager.register(container, this.getEquipmentCommand(attribute), "aaa-equipment");
    }

    private CommandSpec getItemsCommand() {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-items")
                .arguments(GenericArguments.firstParsing(
                        GenericArguments.literal(Text.of("give"), "give"),
                        GenericArguments.literal(Text.of("save"), "save")),
                        GenericArguments.string(Text.of("name")))
                .executor((src, args) -> {
                    String name = args.<String>getOne(Text.of("name")).orElse("null");
                    if (!NAME_PATTERN.matcher(name).matches()) {
                        this.locale.to(src, "commands.byte-items.invalid-name", name);
                        return CommandResult.success();
                    }
                    if (args.hasAny(Text.of("save"))) {
                        if (src instanceof Player) {
                            Player player = (Player) src;
                            Optional<ItemStack> stackOptional = player.getItemInHand(HandTypes.MAIN_HAND);
                            if (stackOptional.isPresent()) {
                                ItemStack stack = stackOptional.get();
                                if (DataUtil.hasData(stack)) {
                                    stack.remove(Keys.ITEM_LORE); // lore texts are generated
                                    Attributes.ID.setValues(stack, ImmutableList.of(StringValue.of(name)));
                                    player.setItemInHand(HandTypes.MAIN_HAND, stack);
                                    this.biHandler.save(name, player);
                                    this.locale.to(src, "commands.byte-items.save-succeed", name, "aaa-" + name);
                                    return CommandResult.success();
                                }
                            }
                        }
                        this.locale.to(src, "commands.drop.nonexist");
                    }
                    if (args.hasAny(Text.of("give"))) {
                        if (src instanceof Player) {
                            Player player = (Player) src;
                            ItemStackSnapshot snapshot = this.biHandler.read(name);
                            if (snapshot.isEmpty()) {
                                this.locale.to(src, "commands.byte-items.nonexist", name, "aaa-" + name);
                                return CommandResult.success();
                            }
                            InventoryTransactionResult result = player.getInventory().offer(snapshot.createStack());
                            if (InventoryTransactionResult.Type.SUCCESS.equals(result.getType())) {
                                this.locale.to(src, "commands.byte-items.give-succeed", name, "aaa-" + name);
                                return CommandResult.success();
                            }
                        }
                        this.locale.to(src, "commands.byte-items.full");
                    }
                    return CommandResult.success();
                })
                .child(CommandSpec.builder()
                        .arguments(GenericArguments.optionalWeak(GenericArguments.remainingJoinedStrings(Text.of("display"))))
                        .executor((src, args) -> {
                            if (src instanceof Player) {
                                Optional<ItemStack> optional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                                if (optional.isPresent()) {
                                    ItemStack stack = optional.get();
                                    String text = args.<String>getOne("display").orElse(null);
                                    if (StringUtils.isNotEmpty(text)) {
                                        stack.offer(Keys.DISPLAY_NAME, TextSerializers.FORMATTING_CODE.deserialize(text));
                                    } else {
                                        stack.remove(Keys.DISPLAY_NAME);
                                    }
                                    return CommandResult.success();
                                }
                            }
                            this.locale.to(src, "commands.drop.nonexist");
                            return CommandResult.success();
                        })
                        .build(), "name")
                .build();
    }

    private CommandSpec getDropCommand(Attribute<Text> attribute) {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-drop")
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        AtomicBoolean isCallbackExecuted = new AtomicBoolean(false);
                        Arg arg = Arg.ref("commands.drop.warning-ok").withCallback(value -> {
                            if (!isCallbackExecuted.getAndSet(true)) {
                                Optional<ItemStack> stackOptional = ((Player) value).getItemInHand(HandTypes.MAIN_HAND);
                                if (stackOptional.isPresent()) {
                                    ItemStack stack = stackOptional.get();
                                    if (DataUtil.hasData(stack)) {
                                        List<Text> lore = attribute.getValues(stack);
                                        DataUtil.dropData(stack);
                                        stack.offer(Keys.ITEM_LORE, lore);
                                        ((Player) value).setItemInHand(HandTypes.MAIN_HAND, stack);
                                        this.locale.to(value, "commands.drop.succeed");
                                        return;
                                    }
                                }
                                this.locale.to(value, "commands.drop.nonexist");
                            }
                        });
                        locale.to(src, "commands.drop.warning", arg);
                        return CommandResult.success();
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    private CommandSpec getInitCommand(Attribute<Text> attribute) {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-init")
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        if (stackOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                this.locale.to(src, "commands.init.already-exist");
                            } else {
                                attribute.setValues(stack, stack.get(Keys.ITEM_LORE).orElse(ImmutableList.of()));
                                ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                this.locale.to(src, "commands.init.succeed");
                            }
                            return CommandResult.success();
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    private <T extends RangeValue> CommandSpec getRangeCommand(String id, boolean fixed, Attribute<T> attribute) {
        CommandElement rangeElement = new RangeValueElement(this.locale, fixed, Text.of("rangeValue"));
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-" + id)
                .child(this.getClearCommand(id, attribute), "clear")
                .child(this.getAppendCommand(id, attribute, rangeElement), "append")
                .child(this.getInsertCommand(id, attribute, rangeElement), "insert")
                .child(this.getPrependCommand(id, attribute, rangeElement), "prepend")
                .build();
    }

    private <T extends DataSerializable> CommandSpec getPrependCommand(String id, Attribute<T> attribute, CommandElement valueElement) {
        return CommandSpec.builder()
                .arguments(valueElement)
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Optional<T> rangeValueOptional = args.getOne(valueElement.getKey());
                        if (stackOptional.isPresent() && rangeValueOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.prependValue(stack, rangeValueOptional.get());
                                ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                this.locale.to(src, "commands.range.prepend-attribute", stack, id);
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    /**
     * get the append command for a attribute value to sth.
     *
     * @param id           the id of the attribute
     * @param attribute    the attribute value
     * @param valueElement the command value element
     * @param <T>          the type of the value
     * @return the command to append the value to the item stack.
     */
    private <T extends DataSerializable> CommandSpec getAppendCommand(String id, Attribute<T> attribute, CommandElement valueElement) {
        return CommandSpec.builder()
                .arguments(valueElement)
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Optional<T> rangeValueOptional = args.getOne(valueElement.getKey());
                        if (stackOptional.isPresent() && rangeValueOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.appendValue(stack, rangeValueOptional.get());
                                ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                this.locale.to(src, "commands.range.append-attribute", stack, id);
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    /**
     * get the insert command for a attribute value to sth.
     *
     * @param id           the id of the attribute
     * @param attribute    the attribute
     * @param valueElement the command value element
     * @param <T>          the type of the value
     * @return the command to insert the value
     */
    private <T extends DataSerializable> CommandSpec getInsertCommand(String id, Attribute<T> attribute, CommandElement valueElement) {
        return CommandSpec.builder()
                .arguments(valueElement, new IndexValueElement(this.locale, Text.of("index")))
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        int index = args.<Integer>getOne(Text.of("index")).orElseThrow(NoSuchElementException::new);
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Optional<T> rangeValueOptional = args.getOne(valueElement.getKey());
                        if (stackOptional.isPresent() && rangeValueOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.insertValue(stack, index, rangeValueOptional.get());
                                ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                this.locale.to(src, "commands.range.append-attribute", stack, id);
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    private <T extends DataSerializable> CommandSpec getClearCommand(String id, Attribute<T> attribute) {
        return CommandSpec.builder()
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        if (stackOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.clearValues(stack);
                                ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                this.locale.to(src, "commands.range.clear-attribute", stack, id);
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    private CommandSpec getMarkerCommand(String id, Attribute<MarkerValue> attribute) {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-" + id)
                .arguments(GenericArguments.choices(Text.of("marked"),
                        ImmutableMap.of("mark", Boolean.TRUE, "unmark", Boolean.FALSE)))
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
                                    this.locale.to(src, "commands.marker.mark-attribute", stack, id);
                                } else {
                                    attribute.clearValues(stack);
                                    ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                    this.locale.to(src, "commands.marker.unmark-attribute", stack, id);
                                }
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    private CommandSpec getPublicizeCommand(Attribute<GameProfile> attribute) {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-publicize")
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        if (stackOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.clearValues(stack);
                                this.locale.to(src, "commands.possess.unmark-attribute");
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    private CommandSpec getPossessCommand(Attribute<GameProfile> attribute) {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-possess")
                .arguments(GenericArguments.optional(GenericArguments.player(Text.of("player"))))
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Player target = args.<Player>getOne(Text.of("player")).orElse((Player) src);
                        if (stackOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.setValues(stack, ImmutableList.of(target.getProfile()));
                                this.locale.to(src, "commands.possess.mark-attribute", target.getName());
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    private CommandSpec getEquipmentCommand(Attribute<StringValue> attribute) {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-equipment")
                .arguments(
                        GenericArguments.choices(Text.of("marked"),
                                ImmutableMap.of("mark", Boolean.TRUE, "unmark", Boolean.FALSE)),
                        GenericArguments.allOf(new EquipmentTypeElement("slots"))
                )
                .child(getClearCommand("equipment", attribute), "clear")
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Player player = (Player) src;
                        Optional<ItemStack> optional = player.getItemInHand(HandTypes.MAIN_HAND);
                        if (optional.isPresent()) {
                            ItemStack stack = optional.get();
                            ImmutableList<StringValue> old = attribute.getValues(stack);
                            boolean mark = args.<Boolean>getOne("marked").orElse(Boolean.FALSE);
                            Collection<StringValue> slots = args.<EquipmentType>getAll("slots").stream()
                                    .map(CatalogType::getId)
                                    .map(StringValue::of)
                                    .collect(Collectors.toList());
                            ImmutableList<StringValue> list = mark
                                    ? ImmutableList.<StringValue>builder().addAll(old).addAll(slots).build()
                                    : ImmutableList.copyOf(old.stream().filter(it -> !slots.contains(it)).iterator());
                            if (!list.isEmpty()) {
                                attribute.setValues(stack, list);
                            } else {
                                attribute.clearValues(stack);
                            }
                            this.locale.to(src, "commands.marker.mark-attribute", stack, "equipment");
                        }
                        return CommandResult.success();
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    private <T extends StringValue> CommandSpec getStringCommand(Attribute<T> attribute, String id) {
        StringValueElement element = new StringValueElement(Text.of("string"));
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-" + id)
                .child(getAppendCommand(id, attribute, element), "append")
                .child(getPrependCommand(id, attribute, element), "prepend")
                .child(getClearCommand(id, attribute), "clear")
                .child(getInsertCommand(id, attribute, element), "insert")
                .build();
    }

    private <T extends StringValue> CommandSpec getTemplateCommand(Attribute<T> attribute, String id) {
        TemplateStringElement element = new TemplateStringElement(Text.of("template"));
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-" + id)
                .child(getAppendCommand(id, attribute, element), "append")
                .child(getPrependCommand(id, attribute, element), "prepend")
                .child(getClearCommand(id, attribute), "clear")
                .child(getInsertCommand(id, attribute, element), "insert")
                .build();
    }
}
