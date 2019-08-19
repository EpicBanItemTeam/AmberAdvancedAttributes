package io.izzel.aaa.listener;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.inject.Singleton;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.service.Attributes;
import io.izzel.aaa.util.EquipmentUtil;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
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
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.type.CarriedInventory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;

@Singleton
public class AttackListener {

    private final Random random = new Random();

    private Optional<Equipable> getBySource(EntityDamageSource source) {
        Entity from = source.getSource();
        if (from instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) from).getShooter();
            if (shooter instanceof Equipable) return Optional.of(((Equipable) shooter));
            else return Optional.empty();
        } else if (from instanceof Equipable) {
            return Optional.of(((Equipable) from));
        } else return Optional.empty();
    }

    @Listener(order = Order.EARLY)
    public void onAttack(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        getBySource(source).ifPresent(from -> {
            // 这个硬编码真是蠢极了，但是提出来又没什么必要
            EquipmentUtil.items(from).forEach(itemStack -> {
                Attributes.ATTACK.getValues(itemStack).forEach(v ->
                        event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause().with(Attributes.ATTACK).with(v))
                                        .type(DamageModifierTypes.WEAPON_ENCHANTMENT).item(itemStack).build(),
                                v.getFunction(this.random), ImmutableSet.of()));
                if (from instanceof Player && to instanceof Player) {
                    Attributes.PVP_ATTACK.getValues(itemStack).forEach(v ->
                            event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause().with(Attributes.PVP_ATTACK).with(v))
                                            .type(DamageModifierTypes.WEAPON_ENCHANTMENT).item(itemStack).build(),
                                    v.getFunction(this.random), ImmutableSet.of()));
                } else if (from instanceof Player) {
                    Attributes.PVE_ATTACK.getValues(itemStack).forEach(v ->
                            event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause().with(Attributes.PVE_ATTACK).with(v))
                                            .type(DamageModifierTypes.WEAPON_ENCHANTMENT).item(itemStack).build(),
                                    v.getFunction(this.random), ImmutableSet.of()));
                }
            });
        });
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
            EquipmentUtil.items(((Equipable) to)).forEach(itemStack -> {
                Attributes.DEFENSE.getValues(itemStack).forEach(v -> {
                    System.out.println(itemStack + " " + v);
                    event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause().with(Attributes.DEFENSE).with(v))
                                    .type(DamageModifierTypes.ARMOR_ENCHANTMENT).item(itemStack).build(),
                            defense(v), ImmutableSet.of());
                });
                if (source instanceof EntityDamageSource) {
                    getBySource(((EntityDamageSource) source)).ifPresent(from -> {
                        if (from instanceof Player && to instanceof Player) {
                            Attributes.PVP_DEFENSE.getValues(itemStack).forEach(v ->
                                    event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause().with(Attributes.PVP_DEFENSE).with(v))
                                                    .type(DamageModifierTypes.ARMOR_ENCHANTMENT).item(itemStack).build(),
                                            defense(v), ImmutableSet.of()));
                        } else if (from instanceof Player) {
                            Attributes.PVE_DEFENSE.getValues(itemStack).forEach(v ->
                                    event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause().with(Attributes.PVE_DEFENSE).with(v))
                                                    .type(DamageModifierTypes.ARMOR_ENCHANTMENT).item(itemStack).build(),
                                            defense(v), ImmutableSet.of()));
                        }
                    });
                }
            });
        }
    }

    @Listener(order = Order.FIRST)
    public void onDodge(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        if (to instanceof Equipable) {
            double dodge = EquipmentUtil.allOf(((Equipable) to), Attributes.DODGE);
            double accuracy = getBySource(source).map(it -> EquipmentUtil.allOf(it, Attributes.ACCURACY)).orElse(0D);
            if (random.nextDouble() < dodge - accuracy) {
                event.setCancelled(true);
            }
        }
    }

    @Listener(order = Order.DEFAULT)
    public void onCrit(DamageEntityEvent event, @First EntityDamageSource source) {
        getBySource(source).ifPresent(from -> {
            double crit = EquipmentUtil.allOf(from, Attributes.CRIT);
            double critRate = EquipmentUtil.allOf(from, Attributes.CRIT_RATE);
            if (random.nextDouble() < critRate) {
                event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                                .type(DamageModifierTypes.CRITICAL_HIT).build(),
                        d -> d * crit, ImmutableSet.of());
            }
        });
    }

    @Listener(order = Order.LATE)
    public void onInstantDeath(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        getBySource(source).ifPresent(from -> {
            double instantDeath = EquipmentUtil.allOf(from, Attributes.INSTANT_DEATH);
            boolean instantImmune = false;
            if (to instanceof Equipable) {
                instantImmune = EquipmentUtil.items(((Equipable) to)).map(Attributes.INSTANT_DEATH_IMMUNE::getValues)
                        .flatMap(Collection::stream)
                        .findAny().isPresent();
            }
            if (!instantImmune && random.nextDouble() < instantDeath) {
                double damage = event.getFinalDamage();
                event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                                .type(DamageModifierTypes.CRITICAL_HIT).build(),
                        d -> to.get(Keys.HEALTH).orElse(damage) - damage, ImmutableSet.of());
            }
        });
    }

    @Listener(order = Order.LAST)
    public void onReflect(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        if (to instanceof Equipable) {
            getBySource(source).ifPresent(from -> {
                double reflectRate = EquipmentUtil.allOf(((Equipable) to), Attributes.REFLECT_RATE);
                if (random.nextDouble() < reflectRate) {
                    double reflect = EquipmentUtil.allOf(((Equipable) to), Attributes.REFLECT);
                    double pvpReflect = 0D, pveReflect = 0D;
                    if (to instanceof Player && from instanceof Player) {
                        pvpReflect = EquipmentUtil.allOf(((Player) to), Attributes.PVP_REFLECT);
                    } else if (to instanceof Player) {
                        pveReflect = EquipmentUtil.allOf(((Player) to), Attributes.PVE_REFLECT);
                    }
                    double total = reflect + pvpReflect + pveReflect;
                    ((Entity) from).damage(event.getFinalDamage() * total,
                            EntityDamageSource.builder().absolute().entity(to).type(DamageTypes.CUSTOM).build());
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    @Listener
    public <T extends Equipable & Entity> void onLoot(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        if (source.getSource() instanceof Equipable && to instanceof Carrier) {
            T from = (T) source.getSource();
            double loot = EquipmentUtil.allOf(from, Attributes.LOOT_RATE);
            if (random.nextDouble() < loot) {
                try {
                    CarriedInventory<? extends Carrier> inventory = ((Carrier) to).getInventory();
                    List<Inventory> slots = Streams.stream(inventory.slots()).collect(Collectors.toList());
                    Inventory slot = slots.get(random.nextInt(slots.size()));
                    slot.poll(1).ifPresent(item -> {
                        if (!Attributes.LOOT_IMMUNE.getValues(item).isEmpty()) {
                            slot.offer(item);
                        } else {
                            Item entityItem = ((Item) from.getLocation().getExtent().createEntity(EntityTypes.ITEM, from.getLocation().getPosition()));
                            entityItem.offer(Keys.REPRESENTED_ITEM, item.createSnapshot());
                            from.getLocation().getExtent().spawnEntity(entityItem);
                        }
                    });
                } catch (Throwable ignored) {
                    // TODO java.lang.AbstractMethodError: Method net/minecraft/entity/monster/EntityZombie.getInventory()Lorg/spongepowered/api/item/inventory/type/CarriedInventory; is abstract
                }
            }
        }
    }

}
