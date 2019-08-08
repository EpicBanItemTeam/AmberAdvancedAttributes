package io.izzel.aaa.listener;

import com.google.inject.Inject;
import io.izzel.aaa.service.Attributes;
import io.izzel.amber.commons.i18n.AmberLocale;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.AffectSlotEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;

import java.util.Collection;
import java.util.Optional;

public class PossessionListener {

    @Inject private AmberLocale locale;

    @Listener
    public void on(AffectSlotEvent event, @Root Player player) {
        if (event.getTransactions().stream()
            .map(SlotTransaction::getSlot)
            .map(Inventory::peek)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Attributes.POSSESSION::getValues)
            .flatMap(Collection::stream)
            .anyMatch(it -> !it.getUniqueId().equals(player.getUniqueId()))
            && !player.hasPermission("aaa.possession-bypass")) {
            event.setCancelled(true);
            locale.to(player, "attributes.possession.no-permission");
        }
    }


}
