package io.izzel.aaa.listener;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.service.Attributes;
import io.izzel.aaa.util.EquipmentUtil;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.item.ImmutableDurabilityData;
import org.spongepowered.api.data.manipulator.mutable.entity.HealthData;
import org.spongepowered.api.data.manipulator.mutable.item.DurabilityData;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.data.Supports;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
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
                for (Player player : game.getServer().getOnlinePlayers()) {
                    double saturation = EquipmentUtil.instance().allOf(player, Attributes.SATURATION);
                    double starvation = EquipmentUtil.instance().allOf(player, Attributes.STARVATION);
                    int food = player.get(Keys.FOOD_LEVEL).orElse(0);
                    player.offer(Keys.FOOD_LEVEL, food + (int) (saturation - starvation));
                    double regen = EquipmentUtil.instance().allOf(player, Attributes.REGENERATION);
                    double max = player.get(Keys.MAX_HEALTH).orElse(0D);
                    double health = player.get(Keys.HEALTH).orElse(0D);
                    player.offer(Keys.HEALTH, Math.min(max, health + regen));
                }
            };
            Task.builder().delayTicks(20).intervalTicks(20).execute(executor).submit(container);
        });
    }

    @Listener
    public void onBurn(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        Entity from = source.getSource();
        if (from instanceof Equipable) {
            double burnRate = EquipmentUtil.instance().allOf(((Equipable) from), Attributes.BURN_RATE);
            if (random.nextDouble() < burnRate) {
                double burn = EquipmentUtil.instance().allOf(((Equipable) from), Attributes.BURN);
                to.offer(Keys.FIRE_TICKS, to.get(Keys.FIRE_TICKS).orElse(0) + (int) burn);
            }
        }
    }

    @Listener
    public void onLifeSteal(DamageEntityEvent event, @Supports(HealthData.class) @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        Entity from = source.getSource();
        if (from instanceof Equipable) {
            double lifeStealRate = EquipmentUtil.instance().allOf(((Equipable) from), Attributes.LIFE_STEAL_RATE);
            if (random.nextDouble() < lifeStealRate) {
                double lifeSteal = EquipmentUtil.instance().allOf(((Equipable) from), Attributes.LIFE_STEAL);
                to.offer(Keys.HEALTH, Math.min(to.get(Keys.MAX_HEALTH).orElse(0D), to.get(Keys.HEALTH).orElse(0D) + lifeSteal));
            }
        }
    }

    @Listener
    public void onKnockback(AttackEntityEvent event, @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        Entity from = source.getSource();
        if (from instanceof Equipable) {
            double knockback = EquipmentUtil.instance().allOf(((Equipable) from), Attributes.KNOCKBACK);
            event.setKnockbackModifier(event.getKnockbackModifier() + (int) knockback);
        }
    }

    @Listener()
    public void on(ChangeEntityEquipmentEvent event) {
        Entity entity = event.getTargetEntity();
        if (entity instanceof Equipable) {
            if (entity.supports(Keys.WALKING_SPEED)) {
                double speed = EquipmentUtil.instance().allOf(((Equipable) entity), Attributes.MOVE_SPEED, DEFAULT_MOVE_SPEED);
                entity.offer(Keys.WALKING_SPEED, speed);
            }
            // TODO double attackSpeed = EquipmentEquipmentUtil.allOf(((Equipable) entity), Attributes.ATTACK_SPEED, 0D);
            //  Keys.ATTACK_SPEED
            if (entity.supports(Keys.MAX_HEALTH)) {
                double max = EquipmentUtil.instance().allOf(((Equipable) entity), Attributes.MAX_HEALTH, DEFAULT_MAX_HEALTH);
                entity.offer(Keys.MAX_HEALTH, max);
            }
            // TODO ((Player) entity).getCooldownTracker()..getCooldown()
        }
        Transaction<ItemStackSnapshot> transaction = event.getTransaction();
        if (transaction.isValid()) {
            ItemStack stack = transaction.getFinal().createStack();
            ItemStackSnapshot originalStack = transaction.getOriginal();
            if (originalStack.getType().equals(stack.getType()) && stack.supports(DurabilityData.class)) {
                DurabilityData newData = stack.get(DurabilityData.class).get();
                ImmutableDurabilityData oldData = originalStack.getOrCreate(ImmutableDurabilityData.class).get();

                if (Attributes.UNBREAKABLE.getValues(stack).isEmpty()) {
                    stack.offer(Keys.HIDE_UNBREAKABLE, Boolean.FALSE);
                    newData.set(newData.unbreakable().set(Boolean.FALSE));

                    List<RangeValue> data = Attributes.DURABILITY.getValues(stack);
                    if (!data.isEmpty()) {
                        MutableBoundedValue<Integer> durability = newData.durability();
                        double lower = data.stream().mapToDouble(RangeValue::getLowerBound).sum();
                        double upper = data.stream().mapToDouble(RangeValue::getUpperBound).sum();

                        lower = lower + durability.get() - oldData.durability().get();
                        Attributes.DURABILITY.setValues(stack, ImmutableList.of(RangeValue.absolute(lower, upper)));

                        stack.offer(newData.set(durability.set(Math.min((int) lower, durability.getMaxValue()))));
                    }
                } else {
                    stack.offer(Keys.HIDE_UNBREAKABLE, Boolean.TRUE);
                    newData.set(newData.unbreakable().set(Boolean.TRUE));

                    List<RangeValue> data = Attributes.DURABILITY.getValues(stack);
                    if (!data.isEmpty()) {
                        MutableBoundedValue<Integer> durability = newData.durability();
                        double upper = data.stream().mapToDouble(RangeValue::getUpperBound).sum();

                        Attributes.DURABILITY.setValues(stack, ImmutableList.of(RangeValue.absolute(upper)));

                        stack.offer(newData.set(durability.set(Math.min((int) upper, durability.getMaxValue()))));
                    }
                }
                transaction.setCustom(stack.createSnapshot());
            }
        }
    }
}
