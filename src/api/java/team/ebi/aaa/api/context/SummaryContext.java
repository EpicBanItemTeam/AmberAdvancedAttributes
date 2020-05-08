package team.ebi.aaa.api.context;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
public interface SummaryContext {
    Player getPlayer();
}
