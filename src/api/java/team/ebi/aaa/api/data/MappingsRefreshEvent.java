package team.ebi.aaa.api.data;

import org.spongepowered.api.event.entity.living.humanoid.player.TargetPlayerEvent;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;
import java.util.Set;

@NonnullByDefault
public interface MappingsRefreshEvent extends TargetPlayerEvent {
    Optional<Mappings> getTargetMappings(TemplateSlot slot);

    Set<? extends TemplateSlot> getAvailableSlots();
}
