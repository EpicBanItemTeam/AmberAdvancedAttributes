package io.izzel.aaa.listener;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.aaa.Util;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.service.Attributes;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.HealthData;
import org.spongepowered.api.data.property.AbstractProperty;
import org.spongepowered.api.data.property.item.UseLimitProperty;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.data.Supports;
import org.spongepowered.api.event.item.inventory.UseItemStackEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;

import java.util.Optional;
import java.util.Random;

@Singleton
public class MiscListener {

    private final Random random = new Random();

    @Inject
    public MiscListener(PluginContainer container) {
        Object plugin = container.getInstance().orElseThrow(RuntimeException::new);
        Task.builder().delayTicks(20).intervalTicks(20).execute(() ->
            Sponge.getServer().getOnlinePlayers().forEach(player -> {
                double saturation = Util.allOf(player, Attributes.SATURATION);
                double starvation = Util.allOf(player, Attributes.STARVATION);
                int food = player.get(Keys.FOOD_LEVEL).orElse(0);
                player.offer(Keys.FOOD_LEVEL, food + (int) (saturation - starvation));
                double regen = Util.allOf(player, Attributes.REGENERATION);
                double max = player.get(Keys.MAX_HEALTH).orElse(0D);
                double health = player.get(Keys.HEALTH).orElse(0D);
                player.offer(Keys.HEALTH, Math.min(max, health + regen));
            })).submit(plugin);
    }

    @Listener
    public void onBurn(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        Entity from = source.getSource();
        if (from instanceof Equipable) {
            double burnRate = Util.allOf(((Equipable) from), Attributes.BURN_RATE);
            if (random.nextDouble() < burnRate) {
                double burn = Util.allOf(((Equipable) from), Attributes.BURN);
                to.offer(Keys.FIRE_TICKS, to.get(Keys.FIRE_TICKS).orElse(0) + (int) burn);
            }
        }
    }

    @Listener
    public void onLifeSteal(DamageEntityEvent event, @Supports(HealthData.class) @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        Entity from = source.getSource();
        if (from instanceof Equipable) {
            double lifeStealRate = Util.allOf(((Equipable) from), Attributes.LIFE_STEAL_RATE);
            if (random.nextDouble() < lifeStealRate) {
                double lifeSteal = Util.allOf(((Equipable) from), Attributes.LIFE_STEAL);
                to.offer(Keys.HEALTH, Math.min(to.get(Keys.MAX_HEALTH).orElse(0D), to.get(Keys.HEALTH).orElse(0D) + lifeSteal));
            }
        }
    }

    @Listener
    public void onKnockback(AttackEntityEvent event, @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        Entity from = source.getSource();
        if (from instanceof Equipable) {
            double knockback = Util.allOf(((Equipable) from), Attributes.KNOCKBACK);
            event.setKnockbackModifier(event.getKnockbackModifier() + (int) knockback);
        }
    }

    private static final double DEFAULT_MOVE_SPEED = 0.7D, DEFAULT_MAX_HEALTH = 20D;

    @Listener
    public void on(ChangeEntityEquipmentEvent event) {
        Entity entity = event.getTargetEntity();
        if (entity instanceof Equipable) {
            if (entity.supports(Keys.WALKING_SPEED)) {
                double speed = Util.allOf(((Equipable) entity), Attributes.MOVE_SPEED, DEFAULT_MOVE_SPEED);
                entity.offer(Keys.WALKING_SPEED, speed);
            }
            // TODO double attackSpeed = Util.allOf(((Equipable) entity), Attributes.ATTACK_SPEED, 0D);
            //  Keys.ATTACK_SPEED
            if (entity.supports(Keys.MAX_HEALTH)) {
                double max = Util.allOf(((Equipable) entity), Attributes.MAX_HEALTH, DEFAULT_MAX_HEALTH);
                entity.offer(Keys.MAX_HEALTH, max);
            }
            // TODO ((Player) entity).getCooldownTracker()..getCooldown()
        }
        Transaction<ItemStackSnapshot> transaction = event.getTransaction();
        if (transaction.isValid()) {
            ItemStack stack = transaction.getFinal().createStack();
            if (!Attributes.UNBREAKABLE.getValues(stack).isEmpty()) {
                stack.offer(Keys.UNBREAKABLE, true);
                stack.offer(Keys.HIDE_UNBREAKABLE, true);
            }
        }
    }

    @Listener
    public void on(UseItemStackEvent event) {
        ItemStack stack = event.getItemStackInUse().createStack();
        stack.getType().getDefaultProperty(UseLimitProperty.class).map(AbstractProperty::getValue)
            .ifPresent(max -> {
                Optional<RangeValue> opt = Attributes.DURABILITY.getValues(stack).stream()
                    .reduce((a, b) -> new RangeValue(a.getLowerBound() + b.getLowerBound(),
                        a.getUpperBound() + b.getUpperBound(), false));
                if (opt.isPresent()) {
                    RangeValue value = new RangeValue(opt.get().getLowerBound() - 1, opt.get().getUpperBound(), false);
                    if (((int) value.getLowerBound()) == 0) {
                        event.setRemainingDuration(0);
                    } else {
                        double p = value.getLowerBound() / value.getUpperBound();
                        int remain = Math.min((int) (p * max), 1);
                        event.setRemainingDuration(remain);
                        Attributes.DURABILITY.setValues(stack, ImmutableList.of(value));
                    }
                }
            });
    }

}
