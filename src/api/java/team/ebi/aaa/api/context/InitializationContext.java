package team.ebi.aaa.api.context;

import team.ebi.aaa.api.data.Template;
import team.ebi.aaa.api.data.TemplateSlot;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
public interface InitializationContext {
    Player getPlayer();

    TemplateSlot getSlot();

    Template getCurrentTemplate();
}
