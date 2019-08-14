package io.izzel.aaa.listener;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import io.izzel.aaa.Util;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.service.Attributes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierTypes;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;

import java.util.Collection;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;

@Singleton
public class AttackListener {

    private final Random random = new Random();

    @Listener(order = Order.EARLY)
    public void onAttack(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        Entity from = source.getSource();
        if (from instanceof Equipable) {
            // 这个硬编码真是蠢极了，但是提出来又没什么必要
            Util.items(((Equipable) from)).forEach(itemStack -> {
                Attributes.ATTACK.getValues(itemStack).forEach(v ->
                    event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                            .type(DamageModifierTypes.WEAPON_ENCHANTMENT).item(itemStack).build(),
                        v.getFunction(this.random), ImmutableSet.of()));
                if (from instanceof Player && to instanceof Player) {
                    Attributes.PVP_ATTACK.getValues(itemStack).forEach(v ->
                        event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                                .type(DamageModifierTypes.WEAPON_ENCHANTMENT).item(itemStack).build(),
                            v.getFunction(this.random), ImmutableSet.of()));
                } else if (from instanceof Player) {
                    Attributes.PVE_ATTACK.getValues(itemStack).forEach(v ->
                        event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                                .type(DamageModifierTypes.WEAPON_ENCHANTMENT).item(itemStack).build(),
                            v.getFunction(this.random), ImmutableSet.of()));
                }
            });
        }
    }

    private DoubleUnaryOperator defense(RangeValue value) {
        if (!value.isRelative()) return d -> Math.max(-value.getFunction(this.random).applyAsDouble(d), 0D);
        else {
            double amount = random.nextBoolean()
                ? value.getLowerBound() + value.getSize() * random.nextDouble()
                : value.getUpperBound() - value.getSize() * random.nextDouble();
            return d -> ((1D / (1D + amount)) - 1D) * d;
        }
    }

    @Listener(order = Order.EARLY)
    public void onDefense(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First DamageSource source) {
        if (to instanceof Equipable) {
            Util.items(((Equipable) to)).forEach(itemStack -> {
                Attributes.DEFENSE.getValues(itemStack).forEach(v ->
                    event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                            .type(DamageModifierTypes.ARMOR_ENCHANTMENT).item(itemStack).build(),
                        defense(v), ImmutableSet.of()));
                if (source instanceof EntityDamageSource) {
                    Entity from = ((EntityDamageSource) source).getSource();
                    if (from instanceof Player && to instanceof Player) {
                        Attributes.PVP_DEFENSE.getValues(itemStack).forEach(v ->
                            event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                                    .type(DamageModifierTypes.ARMOR_ENCHANTMENT).item(itemStack).build(),
                                defense(v), ImmutableSet.of()));
                    } else if (from instanceof Player) {
                        Attributes.PVE_DEFENSE.getValues(itemStack).forEach(v ->
                            event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                                    .type(DamageModifierTypes.ARMOR_ENCHANTMENT).item(itemStack).build(),
                                defense(v), ImmutableSet.of()));
                    }
                }
            });
        }
    }

    @Listener(order = Order.FIRST)
    public void onDodge(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First DamageSource source) {
        if (to instanceof Equipable) {
            double dodge = Util.allOf(((Equipable) to), Attributes.DODGE);
            double accuracy = 0D;
            if (source instanceof EntityDamageSource && ((EntityDamageSource) source).getSource() instanceof Equipable) {
                accuracy = Util.allOf(((Equipable) ((EntityDamageSource) source).getSource()), Attributes.ACCURACY);
            }
            if (random.nextDouble() < dodge - accuracy) {
                event.setCancelled(true);
            }
        }
    }

    @Listener(order = Order.DEFAULT)
    public void onCrit(DamageEntityEvent event, @First EntityDamageSource source) {
        if (source.getSource() instanceof Equipable) {
            double crit = Util.allOf(((Equipable) source.getSource()), Attributes.CRIT);
            double critRate = Util.allOf(((Equipable) source.getSource()), Attributes.CRIT_RATE);
            if (random.nextDouble() < critRate) {
                event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                        .type(DamageModifierTypes.CRITICAL_HIT).build(),
                    d -> d * crit, ImmutableSet.of());
            }
        }
    }

    @Listener(order = Order.LATE)
    public void onInstantDeath(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        Entity from = source.getSource();
        if (from instanceof Equipable) {
            double instantDeath = Util.allOf(((Equipable) from), Attributes.INSTANT_DEATH);
            boolean instantImmune = false;
            if (to instanceof Equipable) {
                instantImmune = Util.items(((Equipable) to)).map(Attributes.INSTANT_DEATH_IMMUNE::getValues)
                    .flatMap(Collection::stream)
                    .findAny().isPresent();
            }
            if (!instantImmune && random.nextDouble() < instantDeath) {
                double damage = event.getFinalDamage();
                event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                        .type(DamageModifierTypes.CRITICAL_HIT).build(),
                    d -> to.get(Keys.HEALTH).orElse(damage) - damage, ImmutableSet.of());
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onReflect(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        Entity from = source.getSource();
        if (to instanceof Equipable) {
            double reflectRate = Util.allOf(((Equipable) to), Attributes.REFLECT_RATE);
            if (random.nextDouble() < reflectRate) {
                double reflect = Util.allOf(((Equipable) to), Attributes.REFLECT);
                double pvpReflect = 0D, pveReflect = 0D;
                if (to instanceof Player && from instanceof Player) {
                    pvpReflect = Util.allOf(((Player) to), Attributes.PVP_REFLECT);
                } else if (to instanceof Player) {
                    pveReflect = Util.allOf(((Player) to), Attributes.PVE_REFLECT);
                }
                double total = reflect + pvpReflect + pveReflect;
                from.damage(event.getFinalDamage() * total, EntityDamageSource.builder().absolute().entity(to).type(DamageTypes.CUSTOM).build());
            }
        }
    }

}
