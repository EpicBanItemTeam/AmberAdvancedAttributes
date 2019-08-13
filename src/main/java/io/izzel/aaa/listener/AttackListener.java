package io.izzel.aaa.listener;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import io.izzel.aaa.Util;
import io.izzel.aaa.service.Attributes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;

import java.util.Collection;
import java.util.Random;

@Singleton
public class AttackListener {

    private final Random random = new Random();

    @Listener
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

    @Listener
    public void onDefense(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First DamageSource source) {
        if (to instanceof Equipable) {
            Util.items(((Equipable) to)).forEach(itemStack -> {
                Attributes.DEFENSE.getValues(itemStack).forEach(v ->
                    event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                            .type(DamageModifierTypes.WEAPON_ENCHANTMENT).item(itemStack).build(),
                        d -> -v.getFunction(this.random).applyAsDouble(d), ImmutableSet.of()));
                if (source instanceof EntityDamageSource) {
                    Entity from = ((EntityDamageSource) source).getSource();
                    if (from instanceof Player && to instanceof Player) {
                        Attributes.PVP_DEFENSE.getValues(itemStack).forEach(v ->
                            event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                                    .type(DamageModifierTypes.WEAPON_ENCHANTMENT).item(itemStack).build(),
                                d -> -v.getFunction(this.random).applyAsDouble(d), ImmutableSet.of()));
                    } else if (from instanceof Player) {
                        Attributes.PVE_DEFENSE.getValues(itemStack).forEach(v ->
                            event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                                    .type(DamageModifierTypes.WEAPON_ENCHANTMENT).item(itemStack).build(),
                                d -> -v.getFunction(this.random).applyAsDouble(d), ImmutableSet.of()));
                    }
                }
            });
        }
    }

    @Listener(order = Order.EARLY)
    public void onDodge(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First DamageSource source) {
        if (to instanceof Equipable) {
            double[] dodge = {0D};
            double[] accuracy = {0D};
            Util.items(((Equipable) to))
                .map(Attributes.DODGE::getValues)
                .flatMap(Collection::stream)
                .map(it -> it.getFunction(random))
                .forEach(it -> dodge[0] += it.applyAsDouble(dodge[0]));
            if (source instanceof EntityDamageSource && ((EntityDamageSource) source).getSource() instanceof Equipable) {
                Util.items(((Equipable) ((EntityDamageSource) source).getSource()))
                    .map(Attributes.ACCURACY::getValues)
                    .flatMap(Collection::stream)
                    .map(it -> it.getFunction(random))
                    .forEach(it -> accuracy[0] += it.applyAsDouble(accuracy[0]));
            }
            if (random.nextDouble() < dodge[0] - accuracy[0]) {
                event.setCancelled(true);
            }
        }
    }

}
