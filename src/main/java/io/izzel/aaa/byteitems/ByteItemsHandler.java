package io.izzel.aaa.byteitems;

import com.google.inject.ProvidedBy;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

/**
 * @author ustc_zzzz
 */
@ProvidedBy(ByteItemsProvider.class)
public interface ByteItemsHandler {
    /**
     * Read item from id
     */
    ItemStackSnapshot read(String id);

    /**
     * Save item in main hand to id and return
     *
     * @throws CommandException if it is failed to save
     */
    ItemStackSnapshot save(String id, Player player) throws CommandException;
}
