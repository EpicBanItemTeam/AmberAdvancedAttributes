package io.izzel.aaa.api;

import io.izzel.aaa.api.data.Mappings;
import io.izzel.aaa.api.data.Template;
import io.izzel.aaa.api.data.TemplateSlot;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@NonnullByDefault
public interface AttributeService {
    static AttributeService instance() {
        return Sponge.getServiceManager().provide(AttributeService.class).orElseThrow(IllegalStateException::new);
    }

    Map<TemplateSlot, Mappings> collectMappings(Player player, boolean refresh);

    default Map<TemplateSlot, Mappings> collectMappings(Player player) {
        return this.collectMappings(player, false);
    }

    Collection<? extends Attribute<?>> getAttributes();

    Optional<Attribute<?>> getAttribute(String deserializationKey);

    Collection<? extends TemplateSlot> getSlots();

    Optional<TemplateSlot> getSlot(Template template);

    default Optional<TemplateSlot> getSlot(String templateString) {
        return Template.tryParse(templateString).flatMap(this::getSlot);
    }
}
