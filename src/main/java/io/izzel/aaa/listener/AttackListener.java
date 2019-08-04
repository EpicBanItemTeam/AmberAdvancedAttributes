package io.izzel.aaa.listener;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import io.izzel.aaa.service.AttributeKeys;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.service.AttributeService;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierTypes;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.equipment.EquipmentInventory;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;

import java.util.Optional;

public class AttackListener {

    @Listener
    public void on(DamageEntityEvent event, @Root Player player) {
        Inventory query = player.getInventory().query(QueryOperationTypes.INVENTORY_TYPE.of(EquipmentInventory.class));
        Streams.stream(query.<Slot>slots()).map(Inventory::peek).filter(Optional::isPresent).map(Optional::get)
            .forEach(itemStack -> AttributeKeys.ATTACK.getValues(itemStack).forEach(v ->
                    event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                            .type(DamageModifierTypes.WEAPON_ENCHANTMENT).item(itemStack).build(),
                        v.getFunction(), ImmutableSet.of())));
    }

}
