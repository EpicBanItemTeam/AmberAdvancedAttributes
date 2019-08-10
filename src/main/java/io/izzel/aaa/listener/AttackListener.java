package io.izzel.aaa.listener;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.inject.Singleton;
import io.izzel.aaa.service.Attributes;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierTypes;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentInventory;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;

import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

@Singleton
public class AttackListener {

    @Listener
    public void on(DamageEntityEvent event, @First Player player) {
        Stream.concat(Streams.stream(player.getInventory().query(QueryOperationTypes.INVENTORY_TYPE.of(EquipmentInventory.class)).slots())
            .map(Inventory::peek), Stream.of(player.getItemInHand(HandTypes.MAIN_HAND), player.getItemInHand(HandTypes.OFF_HAND)))
            .filter(Optional::isPresent).map(Optional::get).forEach(itemStack -> {
            Attributes.ATTACK.getValues(itemStack).forEach(v ->
                event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                        .type(DamageModifierTypes.WEAPON_ENCHANTMENT).item(itemStack).build(),
                    v.getFunction(this.random), ImmutableSet.of()));
        });
    }

    private final Random random = new Random();
}
