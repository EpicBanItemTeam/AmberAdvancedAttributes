package io.izzel.aaa.listener;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.service.Attributes;
import io.izzel.aaa.util.EquipmentUtil;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.item.ImmutableDurabilityData;
import org.spongepowered.api.data.manipulator.mutable.entity.HealthData;
import org.spongepowered.api.data.manipulator.mutable.item.DurabilityData;
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
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;

import java.util.List;
import java.util.Random;

@Singleton
public class MiscListener {

    private static final double DEFAULT_MOVE_SPEED = 0.1D, DEFAULT_MAX_HEALTH = 20D;
    private final Random random = new Random();

    @Inject
    public MiscListener(PluginContainer container, Game game) {
        game.getEventManager().registerListener(container, GameStartingServerEvent.class, event -> {
            Runnable executor = () -> {
                for (var player : game.getServer().getOnlinePlayers()) {
                    if (player.get(Keys.HEALTH).orElse(0D) > 0D) {
                        var saturation = EquipmentUtil.allOf(player, Attributes.SATURATION);
                        var starvation = EquipmentUtil.allOf(player, Attributes.STARVATION);
                        int food = player.get(Keys.FOOD_LEVEL).orElse(0);
                        player.offer(Keys.FOOD_LEVEL, food + (int) (saturation - starvation));
                        var regen = EquipmentUtil.allOf(player, Attributes.REGENERATION);
                        double max = player.get(Keys.MAX_HEALTH).orElse(0D);
                        double health = player.get(Keys.HEALTH).orElse(0D);
                        player.offer(Keys.HEALTH, Math.min(max, health + regen));
                    }
                }
            };
            Task.builder().delayTicks(20).intervalTicks(20).execute(executor).submit(container);
        });
    }

    @Listener
    public void onBurn(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        var from = source.getSource();
        if (from instanceof Equipable) {
            var burnRate = EquipmentUtil.allOf(((Equipable) from), Attributes.BURN_RATE);
            if (random.nextDouble() < burnRate) {
                var burn = EquipmentUtil.allOf(((Equipable) from), Attributes.BURN);
                to.offer(Keys.FIRE_TICKS, to.get(Keys.FIRE_TICKS).orElse(0) + (int) burn);
            }
        }
    }

    @Listener
    public void onLifeSteal(DamageEntityEvent event, @Supports(HealthData.class) @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        var from = source.getSource();
        if (from instanceof Equipable) {
            var lifeStealRate = EquipmentUtil.allOf(((Equipable) from), Attributes.LIFE_STEAL_RATE);
            if (random.nextDouble() < lifeStealRate) {
                var lifeSteal = EquipmentUtil.allOf(((Equipable) from), Attributes.LIFE_STEAL);
                from.offer(Keys.HEALTH, from.get(Keys.HEALTH).orElse(0D) + lifeSteal);
            }
        }
    }

    @Listener
    public void onKnockback(AttackEntityEvent event, @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        var from = source.getSource();
        if (from instanceof Equipable) {
            var knockback = EquipmentUtil.allOf(((Equipable) from), Attributes.KNOCKBACK);
            event.setKnockbackModifier(event.getKnockbackModifier() + (int) knockback);
        }
    }

    @Listener()
    public void on(ChangeEntityEquipmentEvent event) {
        var entity = event.getTargetEntity();
        if (entity instanceof Equipable) {
            if (entity.supports(Keys.WALKING_SPEED)) {
                var speed = EquipmentUtil.allOf(((Equipable) entity), Attributes.MOVE_SPEED, DEFAULT_MOVE_SPEED);
                entity.offer(Keys.WALKING_SPEED, speed);
            }
            // TODO double attackSpeed = EquipmentEquipmentUtil.allOf(((Equipable) entity), Attributes.ATTACK_SPEED, 0D);
            //  Keys.ATTACK_SPEED
            if (entity.supports(Keys.MAX_HEALTH)) {
                var max = EquipmentUtil.allOf(((Equipable) entity), Attributes.MAX_HEALTH, DEFAULT_MAX_HEALTH);
                entity.offer(Keys.MAX_HEALTH, max);
            }
            // TODO ((Player) entity).getCooldownTracker()..getCooldown()
        }
        var transaction = event.getTransaction();
        if (transaction.isValid()) {
            var stack = transaction.getFinal().createStack();
            var originalStack = transaction.getOriginal();
            if (originalStack.getType().equals(stack.getType()) && stack.supports(DurabilityData.class)) {
                var newData = stack.get(DurabilityData.class).get();
                var oldData = originalStack.getOrCreate(ImmutableDurabilityData.class).get();

                if (Attributes.UNBREAKABLE.getValues(stack).isEmpty()) {
                    stack.offer(Keys.HIDE_UNBREAKABLE, Boolean.FALSE);
                    newData.set(newData.unbreakable().set(Boolean.FALSE));

                    List<RangeValue> data = Attributes.DURABILITY.getValues(stack);
                    if (!data.isEmpty()) {
                        var durability = newData.durability();
                        var lower = data.stream().mapToDouble(RangeValue::getLowerBound).sum();
                        var upper = data.stream().mapToDouble(RangeValue::getUpperBound).sum();

                        lower = lower + durability.get() - oldData.durability().get();
                        Attributes.DURABILITY.setValues(stack, ImmutableList.of(RangeValue.absolute(lower, upper)));

                        stack.offer(newData.set(durability.set(Math.min((int) lower, durability.getMaxValue()))));
                    }
                } else {
                    stack.offer(Keys.HIDE_UNBREAKABLE, Boolean.TRUE);
                    newData.set(newData.unbreakable().set(Boolean.TRUE));

                    List<RangeValue> data = Attributes.DURABILITY.getValues(stack);
                    if (!data.isEmpty()) {
                        var durability = newData.durability();
                        var upper = data.stream().mapToDouble(RangeValue::getUpperBound).sum();

                        Attributes.DURABILITY.setValues(stack, ImmutableList.of(RangeValue.absolute(upper)));

                        stack.offer(newData.set(durability.set(Math.min((int) upper, durability.getMaxValue()))));
                    }
                }
                transaction.setCustom(stack.createSnapshot());
            }
        }
    }
}
