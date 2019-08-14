package io.izzel.aaa.service;

import io.izzel.aaa.data.RangeValue;
import org.spongepowered.api.profile.GameProfile;

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
    public static final Attribute<RangeValue.Fixed> UNBREAKABLE;
    public static final Attribute<RangeValue> LOOT_RATE;
    public static final Attribute<RangeValue> LOOT_IMMUNE;
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
    public static final Attribute<RangeValue.Fixed> INSTANT_DEATH_IMMUNE;

    public static final Attribute<GameProfile> POSSESSION;

    private static RuntimeException error() {
        return new RuntimeException("The class is loaded too early! ");
    }

    static {
        AttributeService service = AttributeService.instance();

        ATTACK = service.<RangeValue>getAttributeById("aaa-attack").orElseThrow(Attributes::error);
        POSSESSION = service.<GameProfile>getAttributeById("aaa-possession").orElseThrow(Attributes::error);
        TRACING = service.<RangeValue>getAttributeById("aaa-tracing").orElseThrow(Attributes::error);
        PVP_ATTACK = service.<RangeValue>getAttributeById("aaa-pvp-attack").orElseThrow(Attributes::error);
        PVE_ATTACK = service.<RangeValue>getAttributeById("aaa-pve-attack").orElseThrow(Attributes::error);
        DEFENSE = service.<RangeValue>getAttributeById("aaa-defense").orElseThrow(Attributes::error);
        PVP_DEFENSE = service.<RangeValue>getAttributeById("aaa-pvp-defense").orElseThrow(Attributes::error);
        PVE_DEFENSE = service.<RangeValue>getAttributeById("aaa-pve-defense").orElseThrow(Attributes::error);
        REFLECT = service.<RangeValue>getAttributeById("aaa-reflect").orElseThrow(Attributes::error);
        PVP_REFLECT = service.<RangeValue>getAttributeById("aaa-pvp-reflect").orElseThrow(Attributes::error);
        PVE_REFLECT = service.<RangeValue>getAttributeById("aaa-pve-reflect").orElseThrow(Attributes::error);
        REFLECT_RATE = service.<RangeValue>getAttributeById("aaa-reflect-rate").orElseThrow(Attributes::error);
        CRIT = service.<RangeValue>getAttributeById("aaa-critical").orElseThrow(Attributes::error);
        CRIT_RATE = service.<RangeValue>getAttributeById("aaa-critical-rate").orElseThrow(Attributes::error);
        DODGE = service.<RangeValue>getAttributeById("aaa-dodge").orElseThrow(Attributes::error);
        ACCURACY = service.<RangeValue>getAttributeById("aaa-accuracy").orElseThrow(Attributes::error);
        ACCELERATE = service.<RangeValue>getAttributeById("aaa-accelerate").orElseThrow(Attributes::error);
        ATTACK_SPEED = service.<RangeValue.Fixed>getAttributeById("aaa-attack-speed").orElseThrow(Attributes::error);
        MOVE_SPEED = service.<RangeValue.Fixed>getAttributeById("aaa-move-speed").orElseThrow(Attributes::error);
        DURABILITY = service.<RangeValue>getAttributeById("aaa-durability").orElseThrow(Attributes::error);
        UNBREAKABLE = service.<RangeValue.Fixed>getAttributeById("aaa-unbreakable").orElseThrow(Attributes::error);
        LOOT_RATE = service.<RangeValue>getAttributeById("aaa-loot-rate").orElseThrow(Attributes::error);
        LOOT_IMMUNE = service.<RangeValue>getAttributeById("aaa-loot-immune").orElseThrow(Attributes::error);
        BURN = service.<RangeValue>getAttributeById("aaa-burn").orElseThrow(Attributes::error);
        BURN_RATE = service.<RangeValue>getAttributeById("aaa-burn-rate").orElseThrow(Attributes::error);
        LIFE_STEAL = service.<RangeValue>getAttributeById("aaa-life-steal").orElseThrow(Attributes::error);
        LIFE_STEAL_RATE = service.<RangeValue>getAttributeById("aaa-life-steal-rate").orElseThrow(Attributes::error);
        MAX_HEALTH = service.<RangeValue.Fixed>getAttributeById("aaa-max-health").orElseThrow(Attributes::error);
        ATTACK_RANGE = service.<RangeValue.Fixed>getAttributeById("aaa-attack-range").orElseThrow(Attributes::error);
        STARVATION = service.<RangeValue>getAttributeById("aaa-starvation").orElseThrow(Attributes::error);
        SATURATION = service.<RangeValue>getAttributeById("aaa-saturation").orElseThrow(Attributes::error);
        REGENERATION = service.<RangeValue>getAttributeById("aaa-regeneration").orElseThrow(Attributes::error);
        KNOCKBACK = service.<RangeValue>getAttributeById("aaa-knockback").orElseThrow(Attributes::error);
        INSTANT_DEATH = service.<RangeValue>getAttributeById("aaa-instant-death").orElseThrow(Attributes::error);
        INSTANT_DEATH_IMMUNE = service.<RangeValue.Fixed>getAttributeById("aaa-instant-death-immune").orElseThrow(Attributes::error);
    }

    private Attributes() {
        throw new UnsupportedOperationException();
    }
}
