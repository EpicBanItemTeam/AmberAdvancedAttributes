package io.izzel.aaa.command;

import com.google.common.collect.*;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import io.izzel.aaa.AmberAdvancedAttributes;
import io.izzel.aaa.byteitems.ByteItemsHandler;
import io.izzel.aaa.command.elements.*;
import io.izzel.aaa.data.InlayData;
import io.izzel.aaa.data.MarkerValue;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.data.StringValue;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.AttributeService;
import io.izzel.aaa.service.AttributeToLoreFunction;
import io.izzel.aaa.util.DataUtil;
import io.izzel.amber.commons.i18n.AmberLocale;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
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
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.*;
import java.util.stream.Collectors;

import static io.izzel.aaa.service.AttributeToLoreFunctions.*;

/**
 * @author ustc_zzzz
 */
@SuppressWarnings("SameParameterValue")
@Singleton
public class AttributeCommands {
    public static final Text LORE_SEPARATOR = Text.of();
    private final ByteItemsHandler biHandler;
    private final PluginContainer container;
    private final CommandManager commandManager;
    private final AmberLocale locale;
    private final Injector injector;
    private final NonMarkerValueCommand nonMarkerCommand;

    @Inject
    public AttributeCommands(PluginContainer container, ByteItemsHandler biHandler, CommandManager c,
                             EventManager eventManager, AmberLocale locale, Injector injector, NonMarkerValueCommand n) {
        this.container = container;
        this.biHandler = biHandler;
        this.commandManager = c;
        this.locale = locale;
        this.injector = injector;
        this.nonMarkerCommand = n;
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
        this.commandManager.register(container, this.injector.getInstance(ItemCommand.class).callable(), "aaa-items");
    }

    private void registerPermissionCap(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<StringValue> function = permissionCap(this.locale);
        Attribute<StringValue> attribute = event.register("aaa-" + id, StringValue.class, function);
        this.commandManager.register(container,
                this.nonMarkerCommand.callable(attribute, id, new StringValueElement(Text.of("string"))),
                "aaa-" + id);
    }

    private void registerInlay(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<InlayData> function = inlay(this.locale, this.biHandler);
        Attribute<InlayData> attribute = event.register("aaa-" + id, InlayData.class, function);
        this.commandManager.register(container,
                this.nonMarkerCommand.callable(attribute, id, new InlayDataElement(id)),
                "aaa-" + id);
    }

    private void registerCustomTextValue(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<Text> function = (values, equipable) -> values.stream()
                .map(text -> Maps.immutableEntry((byte) 0, (Text) text))
                .collect(Collectors.toList());
        Attribute<Text> attribute = event.register("aaa-" + id, Text.class, function);
        this.commandManager.register(container,
                this.nonMarkerCommand.callable(attribute, id, GenericArguments.text(Text.of("lore"), TextSerializers.FORMATTING_CODE, true))
                , "aaa-" + id);
    }

    private void registerTemplate(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<StringValue> function = template(this.locale, this.biHandler);
        Attribute<StringValue> attribute = event.register("aaa-" + id, StringValue.class, function);
        this.commandManager.register(container,
                this.nonMarkerCommand.callable(attribute, id, new TemplateStringElement(Text.of("template"))),
                "aaa-" + id);
    }

    private void registerSuit(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<StringValue> function = suit(this.locale, this.biHandler);
        Attribute<StringValue> attribute = event.register("aaa-" + id, StringValue.class, function);
        this.commandManager.register(container,
                this.nonMarkerCommand.callable(attribute, id, new StringValueElement(Text.of("string"))),
                "aaa-" + id);
    }

    private void registerTextValue(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<Text> function = (values, equipable) -> ImmutableList.of();
        Attribute<Text> attribute = event.register("aaa-" + id, Text.class, function);
        InitDropCommand command = this.injector.getInstance(InitDropCommand.class);
        this.commandManager.register(container, command.init(attribute), "aaa-init");
        this.commandManager.register(container, command.drop(attribute), "aaa-drop");
    }

    private void registerDurabilityValue(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<RangeValue> function = durability(this.locale);
        Attribute<RangeValue> attribute = event.register("aaa-" + id, RangeValue.class, function);
        this.commandManager.register(container,
                this.nonMarkerCommand.callable(attribute, id, new RangeValueElement(this.locale, false, Text.of("rangeValue"))),
                "aaa-" + id);
    }

    private void registerRangeValue(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<RangeValue> function = rangeValue(this.locale, id);
        Attribute<RangeValue> attribute = event.register("aaa-" + id, RangeValue.class, function);
        this.commandManager.register(container,
                this.nonMarkerCommand.callable(attribute, id, new RangeValueElement(this.locale, false, Text.of("rangeValue"))),
                "aaa-" + id);
    }

    private void registerRangeValueFixed(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<RangeValue.Fixed> function = rangeValue(this.locale, id);
        Attribute<RangeValue.Fixed> attribute = event.register("aaa-" + id, RangeValue.Fixed.class, function);
        this.commandManager.register(container,
                this.nonMarkerCommand.callable(attribute, id, new RangeValueElement(this.locale, true, Text.of("rangeValue"))),
                "aaa-" + id);
    }

    private void registerMarkerValue(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<MarkerValue> function = markerValue(this.locale, id);
        Attribute<MarkerValue> attribute = event.register("aaa-" + id, MarkerValue.class, function);
        this.commandManager.register(container, this.getMarkerCommand(id, attribute), "aaa-" + id);
    }

    private void registerPossessValue(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<GameProfile> function = profile(this.locale);
        Attribute<GameProfile> attribute = event.register("aaa-" + id, GameProfile.class, function);
        PossessCommand command = this.injector.getInstance(PossessCommand.class);
        this.commandManager.register(container, command.possess(attribute), "aaa-possess");
        this.commandManager.register(container, command.publicize(attribute), "aaa-publicize");
    }

    private void registerEquipment(PluginContainer container, Attribute.RegistryEvent event) {
        AttributeToLoreFunction<StringValue> function = equipment(this.locale);
        Attribute<StringValue> attribute = event.register("aaa-equipment", StringValue.class, function);
        this.commandManager.register(container, this.getEquipmentCommand(attribute), "aaa-equipment");
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

    private CommandSpec getEquipmentCommand(Attribute<StringValue> attribute) {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-equipment")
                .arguments(
                        GenericArguments.choices(Text.of("marked"),
                                ImmutableMap.of("mark", Boolean.TRUE, "unmark", Boolean.FALSE)),
                        GenericArguments.allOf(new EquipmentTypeElement("slots"))
                )
                .child(nonMarkerCommand.clear("equipment", attribute), "clear")
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
                            if (list.isEmpty()) {
                                attribute.clearValues(stack);
                            } else {
                                attribute.setValues(stack, list);
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

}
