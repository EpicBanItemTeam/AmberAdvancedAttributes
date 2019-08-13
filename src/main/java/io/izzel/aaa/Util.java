package io.izzel.aaa;

import com.google.common.collect.Streams;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.equipment.EquipmentInventory;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;

import java.util.Optional;
import java.util.stream.Stream;

public class Util {

    public static Stream<ItemStack> items(Player player) {
        return Stream.concat(Streams.stream(player.getInventory().query(QueryOperationTypes.INVENTORY_TYPE.of(EquipmentInventory.class)).slots())
            .map(Inventory::peek), Stream.of(player.getItemInHand(HandTypes.MAIN_HAND), player.getItemInHand(HandTypes.OFF_HAND)))
            .filter(Optional::isPresent).map(Optional::get);
    }

}
