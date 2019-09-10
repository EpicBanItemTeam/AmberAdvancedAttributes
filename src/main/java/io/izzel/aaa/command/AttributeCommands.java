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
                this.command.callable(attribute, id, new StringValueElement(Text.of("string"))),
                "aaa-" + id);
    }

    private void registerInlay(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<InlayData> function = inlay(this.locale, this.biHandler);
        Attribute<InlayData> attribute = event.register("aaa-" + id, InlayData.class, function);
        this.commandManager.register(container,
                this.command.callable(attribute, id, new InlayDataElement(id)),
                "aaa-" + id);
    }

    private void registerCustomTextValue(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<Text> function = (values, equipable) -> values.stream()
                .map(text -> Maps.immutableEntry((byte) 0, (Text) text))
                .collect(Collectors.toList());
        Attribute<Text> attribute = event.register("aaa-" + id, Text.class, function);
        this.commandManager.register(container,
                this.command.callable(attribute, id, GenericArguments.text(Text.of("lore"), TextSerializers.FORMATTING_CODE, true))
                , "aaa-" + id);
    }

    private void registerTemplate(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<StringValue> function = template(this.locale, this.biHandler);
        Attribute<StringValue> attribute = event.register("aaa-" + id, StringValue.class, function);
        this.commandManager.register(container,
                this.command.callable(attribute, id, new TemplateStringElement(Text.of("template"))),
                "aaa-" + id);
    }

    private void registerSuit(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<StringValue> function = suit(this.locale, this.biHandler);
        Attribute<StringValue> attribute = event.register("aaa-" + id, StringValue.class, function);
        this.commandManager.register(container,
                this.command.callable(attribute, id, new StringValueElement(Text.of("string"))),
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
                this.command.callable(attribute, id, new RangeValueElement(this.locale, false, Text.of("rangeValue"))),
                "aaa-" + id);
    }

    private void registerRangeValue(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<RangeValue> function = rangeValue(this.locale, id);
        Attribute<RangeValue> attribute = event.register("aaa-" + id, RangeValue.class, function);
        this.commandManager.register(container,
                this.command.callable(attribute, id, new RangeValueElement(this.locale, false, Text.of("rangeValue"))),
                "aaa-" + id);
    }

    private void registerRangeValueFixed(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<RangeValue.Fixed> function = rangeValue(this.locale, id);
        Attribute<RangeValue.Fixed> attribute = event.register("aaa-" + id, RangeValue.Fixed.class, function);
        this.commandManager.register(container,
                this.command.callable(attribute, id, new RangeValueElement(this.locale, true, Text.of("rangeValue"))),
                "aaa-" + id);
    }

    private void registerMarkerValue(PluginContainer container, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<MarkerValue> function = markerValue(this.locale, id);
        Attribute<MarkerValue> attribute = event.register("aaa-" + id, MarkerValue.class, function);
        this.commandManager.register(container, this.command.marker(id, attribute), "aaa-" + id);
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
        this.commandManager.register(container, this.injector.getInstance(EquipmentCommand.class).callable(attribute), "aaa-equipment");
    }

}
