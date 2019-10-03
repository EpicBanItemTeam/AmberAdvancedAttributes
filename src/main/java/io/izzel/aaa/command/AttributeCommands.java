package io.izzel.aaa.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import io.izzel.aaa.byteitems.ByteItemsHandler;
import io.izzel.aaa.command.elements.InlayDataElement;
import io.izzel.aaa.command.elements.RangeValueElement;
import io.izzel.aaa.command.elements.StringValueElement;
import io.izzel.aaa.command.elements.TemplateStringElement;
import io.izzel.aaa.data.InlayData;
import io.izzel.aaa.data.MarkerValue;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.data.StringValue;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.AttributeToLoreFunction;
import io.izzel.amber.commons.i18n.AmberLocale;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.stream.Collectors;

import static io.izzel.aaa.service.AttributeToLoreFunctions.*;

/**
 * @author ustc_zzzz
 */
@SuppressWarnings("SameParameterValue")
@Singleton
public class AttributeCommands {
    private final ByteItemsHandler biHandler;
    private final PluginContainer container;
    private final CommandManager commandManager;
    private final AmberLocale locale;
    private final Injector injector;
    private final ValueCommand command;

    @Inject
    public AttributeCommands(PluginContainer container, ByteItemsHandler biHandler, CommandManager c,
                             EventManager eventManager, AmberLocale locale, Injector injector, ValueCommand v) {
        this.container = container;
        this.biHandler = biHandler;
        this.commandManager = c;
        this.locale = locale;
        this.injector = injector;
        this.command = v;
        eventManager.registerListener(container, Attribute.RegistryEvent.class, Order.EARLY, this::on);
    }

    private void on(Attribute.RegistryEvent event) {
        this.registerRangeValue(event, "attack");
        this.registerRangeValue(event, "tracing");
        this.registerRangeValue(event, "pvp-attack");
        this.registerRangeValue(event, "pve-attack");
        this.registerRangeValue(event, "defense");
        this.registerRangeValue(event, "pvp-defense");
        this.registerRangeValue(event, "pve-defense");
        this.registerRangeValue(event, "reflect");
        this.registerRangeValue(event, "pvp-reflect");
        this.registerRangeValue(event, "pve-reflect");
        this.registerRangeValue(event, "reflect-rate");
        this.registerRangeValue(event, "critical");
        this.registerRangeValue(event, "critical-rate");
        this.registerRangeValue(event, "dodge");
        this.registerRangeValue(event, "accuracy");
        this.registerRangeValue(event, "accelerate");
        this.registerRangeValueFixed(event, "attack-speed");
        this.registerRangeValueFixed(event, "move-speed");
        this.registerDurabilityValue(event, "durability");
        this.registerMarkerValue(event, "unbreakable");
        this.registerRangeValue(event, "loot-rate");
        this.registerMarkerValue(event, "loot-immune");
        this.registerRangeValue(event, "burn");
        this.registerRangeValue(event, "burn-rate");
        this.registerRangeValue(event, "life-steal");
        this.registerRangeValue(event, "life-steal-rate");
        this.registerRangeValueFixed(event, "max-health");
        this.registerRangeValueFixed(event, "attack-range");
        this.registerRangeValue(event, "starvation");
        this.registerRangeValue(event, "saturation");
        this.registerRangeValue(event, "regeneration");
        this.registerRangeValue(event, "knockback");
        this.registerRangeValue(event, "instant-death");
        this.registerMarkerValue(event, "instant-death-immune");
        this.registerPossessValue(event, "possession");
        this.registerTextValue(event, "original-lore");
        this.registerEquipment(event);
        this.registerSuit(event, "suit");
        this.registerTemplate(event, "template");
        this.registerCustomTextValue(event, "custom-lore");
        this.registerInlay(event, "inlay");
        this.registerMarkerValue(event, "inlay-gem");
        this.registerRangeValueFixed(event, "inlay-success");
        this.registerPermissionCap(event, "permission-cap");
        this.registerRangeValue(event, "level-cap");
        this.registerItemsCommand();
        this.registerNoLore(event, "no-lore");
        this.registerLoreTemplate(event, "lore-template");

        event.register("aaa-id", StringValue.class, (v, e) -> ImmutableList.of());
    }

    private void registerItemsCommand() {
        this.commandManager.register(this.container, this.injector.getInstance(ItemCommand.class).callable(), "aaa-items");
    }

    private void registerLoreTemplate(Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<StringValue> function = (a, b) -> ImmutableList.of();
        Attribute<StringValue> attribute = event.register("aaa-" + id, StringValue.class, function);
        this.commandManager.register(this.container,
                this.command.callable(attribute, id, new StringValueElement(Text.of("string"))),
                "aaa-" + id);
    }

    private void registerNoLore(Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<MarkerValue> function = markerValue(this.locale, id);
        Attribute<MarkerValue> attribute = event.register("aaa-" + id, MarkerValue.class, function);
        this.commandManager.register(this.container, this.command.noLore(id, attribute), "aaa-" + id);
    }

    private void registerPermissionCap(Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<StringValue> function = permissionCap(this.locale);
        Attribute<StringValue> attribute = event.register("aaa-" + id, StringValue.class, function);
        this.commandManager.register(this.container,
                this.command.callable(attribute, id, new StringValueElement(Text.of("string"))),
                "aaa-" + id);
    }

    private void registerInlay(Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<InlayData> function = inlay(this.locale, this.biHandler);
        Attribute<InlayData> attribute = event.register("aaa-" + id, InlayData.class, function);
        this.commandManager.register(this.container,
                this.command.callable(attribute, id, new InlayDataElement(id)),
                "aaa-" + id);
    }

    private void registerCustomTextValue(Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<Text> function = (values, equipable) -> values.stream()
                .map(text -> Maps.immutableEntry((byte) 0, (Text) text))
                .collect(Collectors.toList());
        Attribute<Text> attribute = event.register("aaa-" + id, Text.class, function);
        this.commandManager.register(this.container,
                this.command.callable(attribute, id, GenericArguments.text(Text.of("lore"), TextSerializers.FORMATTING_CODE, true))
                , "aaa-" + id);
    }

    private void registerTemplate(Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<StringValue> function = template(this.locale, this.biHandler);
        Attribute<StringValue> attribute = event.register("aaa-" + id, StringValue.class, function);
        this.commandManager.register(this.container,
                this.command.callable(attribute, id, new TemplateStringElement(Text.of("template"))),
                "aaa-" + id);
    }

    private void registerSuit(Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<StringValue> function = suit(this.locale, this.biHandler);
        Attribute<StringValue> attribute = event.register("aaa-" + id, StringValue.class, function);
        this.commandManager.register(this.container,
                this.command.callable(attribute, id, new StringValueElement(Text.of("string"))),
                "aaa-" + id);
    }

    private void registerTextValue(Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<Text> function = (values, equipable) -> ImmutableList.of();
        Attribute<Text> attribute = event.register("aaa-" + id, Text.class, function);
        InitDropCommand command = this.injector.getInstance(InitDropCommand.class);
        this.commandManager.register(this.container, command.init(attribute), "aaa-init");
        this.commandManager.register(this.container, command.drop(attribute), "aaa-drop");
    }

    private void registerDurabilityValue(Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<RangeValue> function = durability(this.locale);
        Attribute<RangeValue> attribute = event.register("aaa-" + id, RangeValue.class, function);
        this.commandManager.register(this.container,
                this.command.callable(attribute, id, new RangeValueElement(this.locale, false, Text.of("rangeValue"))),
                "aaa-" + id);
    }

    private void registerRangeValue(Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<RangeValue> function = rangeValue(this.locale, id);
        Attribute<RangeValue> attribute = event.register("aaa-" + id, RangeValue.class, function);
        this.commandManager.register(this.container,
                this.command.callable(attribute, id, new RangeValueElement(this.locale, false, Text.of("rangeValue"))),
                "aaa-" + id);
    }

    private void registerRangeValueFixed(Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<RangeValue.Fixed> function = rangeValue(this.locale, id);
        Attribute<RangeValue.Fixed> attribute = event.register("aaa-" + id, RangeValue.Fixed.class, function);
        this.commandManager.register(this.container,
                this.command.callable(attribute, id, new RangeValueElement(this.locale, true, Text.of("rangeValue"))),
                "aaa-" + id);
    }

    private void registerMarkerValue(Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<MarkerValue> function = markerValue(this.locale, id);
        Attribute<MarkerValue> attribute = event.register("aaa-" + id, MarkerValue.class, function);
        this.commandManager.register(this.container, this.command.marker(id, attribute), "aaa-" + id);
    }

    private void registerPossessValue(Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<GameProfile> function = profile(this.locale);
        Attribute<GameProfile> attribute = event.register("aaa-" + id, GameProfile.class, function);
        PossessCommand command = this.injector.getInstance(PossessCommand.class);
        this.commandManager.register(this.container, command.possess(attribute), "aaa-possess");
        this.commandManager.register(this.container, command.publicize(attribute), "aaa-publicize");
    }

    private void registerEquipment(Attribute.RegistryEvent event) {
        AttributeToLoreFunction<StringValue> function = equipment(this.locale);
        Attribute<StringValue> attribute = event.register("aaa-equipment", StringValue.class, function);
        this.commandManager.register(this.container, this.injector.getInstance(EquipmentCommand.class).callable(attribute), "aaa-equipment");
    }

}
