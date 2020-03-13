package io.izzel.aaa.api.context;

import io.izzel.aaa.api.data.TemplateSlot;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;

@NonnullByDefault
public interface SummaryContext {
    Player getPlayer();

    List<? extends TemplateSlot> getIterationOrder();
}
