package team.ebi.aaa.api.context;

import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import team.ebi.aaa.api.data.Template;
import team.ebi.aaa.api.data.TemplateSlot;

@NonnullByDefault
public interface InitializationContext {
    User getUser();

    TemplateSlot getSlot();

    Template getCurrentTemplate();
}
