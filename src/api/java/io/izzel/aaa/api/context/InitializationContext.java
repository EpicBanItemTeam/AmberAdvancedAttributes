package io.izzel.aaa.api.context;

import io.izzel.aaa.api.data.Template;
import io.izzel.aaa.api.data.TemplateSlot;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
public interface InitializationContext {
    Player getPlayer();

    TemplateSlot getSlot();

    Template getCurrentTemplate();
}
