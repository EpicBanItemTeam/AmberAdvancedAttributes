package io.izzel.aaa.service;

import io.izzel.aaa.data.InlayData;
import io.izzel.aaa.data.MarkerValue;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.data.StringValue;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;

public final class Attributes {
    public static final Attribute<RangeValue> ATTACK;
    public static final Attribute<RangeValue> PVP_ATTACK;
    public static final Attribute<RangeValue> PVE_ATTACK;
    public static final Attribute<RangeValue> DEFENSE;
    public static final Attribute<RangeValue> PVP_DEFENSE;
    public static final Attribute<RangeValue> PVE_DEFENSE;
    public static final Attribute<RangeValue> REFLECT;
    public static final Attribute<RangeValue> PVP_REFLECT;
    public static final Attribute<RangeValue> PVE_REFLECT;
    public static final Attribute<RangeValue> REFLECT_RATE;

    public static final Attribute<RangeValue> CRIT;
    public static final Attribute<RangeValue> CRIT_RATE;
    public static final Attribute<RangeValue> DODGE;
    public static final Attribute<RangeValue> ACCURACY;
    public static final Attribute<RangeValue> TRACING;
    public static final Attribute<RangeValue> ACCELERATE;

    public static final Attribute<RangeValue.Fixed> ATTACK_SPEED; // todo
    public static final Attribute<RangeValue.Fixed> MOVE_SPEED;
    public static final Attribute<RangeValue> DURABILITY;
    public static final Attribute<MarkerValue> UNBREAKABLE;
    public static final Attribute<RangeValue> LOOT_RATE;
    public static final Attribute<MarkerValue> LOOT_IMMUNE;
    public static final Attribute<RangeValue> BURN;
    public static final Attribute<RangeValue> BURN_RATE;
    public static final Attribute<RangeValue> LIFE_STEAL; // 他 ...
    public static final Attribute<RangeValue> LIFE_STEAL_RATE;
    public static final Attribute<RangeValue.Fixed> MAX_HEALTH;
    public static final Attribute<RangeValue.Fixed> ATTACK_RANGE;
    public static final Attribute<RangeValue> STARVATION;
    public static final Attribute<RangeValue> SATURATION;
    public static final Attribute<RangeValue> REGENERATION;
    public static final Attribute<RangeValue> KNOCKBACK;
    public static final Attribute<RangeValue> INSTANT_DEATH;
    public static final Attribute<MarkerValue> INSTANT_DEATH_IMMUNE;

    public static final Attribute<GameProfile> POSSESSION;

    public static final Attribute<Text> ORIGINAL_LORE;

    // Alpha.2
    public static final Attribute<StringValue> EQUIPMENT;
    public static final Attribute<StringValue> SUIT;
    public static final Attribute<StringValue> TEMPLATE;

    // Alpha.3
    public static final Attribute<InlayData> INLAY;
    public static final Attribute<MarkerValue> INLAY_GEM;
    public static final Attribute<RangeValue.Fixed> INLAY_SUCCESS;
    public static final Attribute<StringValue> ID;
    public static final Attribute<StringValue> PERMISSION_CAP;
    public static final Attribute<RangeValue> LEVEL_CAP;
    public static final Attribute<Text> CUSTOM_LORE;

    // Alpha.4
    public static final Attribute<MarkerValue> NO_LORE;

    static {
        var service = AttributeService.instance();

        ATTACK = getAttributeById(service, "aaa-attack");
        TRACING = getAttributeById(service, "aaa-tracing");
        PVP_ATTACK = getAttributeById(service, "aaa-pvp-attack");
        PVE_ATTACK = getAttributeById(service, "aaa-pve-attack");
        DEFENSE = getAttributeById(service, "aaa-defense");
        PVP_DEFENSE = getAttributeById(service, "aaa-pvp-defense");
        PVE_DEFENSE = getAttributeById(service, "aaa-pve-defense");
        REFLECT = getAttributeById(service, "aaa-reflect");
        PVP_REFLECT = getAttributeById(service, "aaa-pvp-reflect");
        PVE_REFLECT = getAttributeById(service, "aaa-pve-reflect");
        REFLECT_RATE = getAttributeById(service, "aaa-reflect-rate");

        CRIT = getAttributeById(service, "aaa-critical");
        CRIT_RATE = getAttributeById(service, "aaa-critical-rate");
        DODGE = getAttributeById(service, "aaa-dodge");
        ACCURACY = getAttributeById(service, "aaa-accuracy");
        ACCELERATE = getAttributeById(service, "aaa-accelerate");

        ATTACK_SPEED = getAttributeById(service, "aaa-attack-speed");
        MOVE_SPEED = getAttributeById(service, "aaa-move-speed");
        DURABILITY = getAttributeById(service, "aaa-durability");
        UNBREAKABLE = getAttributeById(service, "aaa-unbreakable");
        LOOT_RATE = getAttributeById(service, "aaa-loot-rate");
        LOOT_IMMUNE = getAttributeById(service, "aaa-loot-immune");
        BURN = getAttributeById(service, "aaa-burn");
        BURN_RATE = getAttributeById(service, "aaa-burn-rate");
        LIFE_STEAL = getAttributeById(service, "aaa-life-steal");
        LIFE_STEAL_RATE = getAttributeById(service, "aaa-life-steal-rate");
        MAX_HEALTH = getAttributeById(service, "aaa-max-health");
        ATTACK_RANGE = getAttributeById(service, "aaa-attack-range");
        STARVATION = getAttributeById(service, "aaa-starvation");
        SATURATION = getAttributeById(service, "aaa-saturation");
        REGENERATION = getAttributeById(service, "aaa-regeneration");
        KNOCKBACK = getAttributeById(service, "aaa-knockback");
        INSTANT_DEATH = getAttributeById(service, "aaa-instant-death");
        INSTANT_DEATH_IMMUNE = getAttributeById(service, "aaa-instant-death-immune");

        POSSESSION = getAttributeById(service, "aaa-possession");

        ORIGINAL_LORE = getAttributeById(service, "aaa-original-lore");

        EQUIPMENT = getAttributeById(service, "aaa-equipment");
        SUIT = getAttributeById(service, "aaa-suit");
        TEMPLATE = getAttributeById(service, "aaa-template");

        INLAY = getAttributeById(service, "aaa-inlay");
        INLAY_GEM = getAttributeById(service, "aaa-inlay-gem");
        INLAY_SUCCESS = getAttributeById(service, "aaa-inlay-success");
        ID = getAttributeById(service, "aaa-id");
        PERMISSION_CAP = getAttributeById(service, "aaa-permission-cap");
        LEVEL_CAP = getAttributeById(service, "aaa-level-cap");
        CUSTOM_LORE = getAttributeById(service, "aaa-custom-lore");

        NO_LORE = getAttributeById(service, "aaa-no-lore");
    }

    private Attributes() {
        throw new UnsupportedOperationException();
    }

    private static <T extends DataSerializable> Attribute<T> getAttributeById(AttributeService s, String id) {
        return s.<T>getAttributeById(id).orElseThrow(() -> new RuntimeException("The class is loaded too early! "));
    }
}
