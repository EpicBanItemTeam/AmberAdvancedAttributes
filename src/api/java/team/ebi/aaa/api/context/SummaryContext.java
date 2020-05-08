package team.ebi.aaa.api.context;

import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
public interface SummaryContext {
    User getUser();
}
