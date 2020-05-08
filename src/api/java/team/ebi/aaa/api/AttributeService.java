package team.ebi.aaa.api;

import team.ebi.aaa.api.data.Mappings;
import team.ebi.aaa.api.data.Template;
import team.ebi.aaa.api.data.TemplateSlot;
import team.ebi.aaa.api.data.UnreachableSlotDataException;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@NonnullByDefault
public interface AttributeService {
    static AttributeService instance() {
        return Sponge.getServiceManager().provide(AttributeService.class).orElseThrow(IllegalStateException::new);
    }

    /* --- mappings --- */

    Map<TemplateSlot, Mappings> collectMappings(Player player, boolean refresh);

    default Map<TemplateSlot, Mappings> collectMappings(Player player) {
        return this.collectMappings(player, false);
    }

    /* --- attributes --- */

    Collection<? extends Attribute<?>> getAttributes();

    Optional<Attribute<?>> getAttribute(String deserializationKey);

    /* --- template slots --- */

    Collection<? extends TemplateSlot> getSlots();

    Optional<TemplateSlot> getSlot(Template template);

    default Optional<TemplateSlot> getSlot(String templateString) {
        return Template.tryParse(templateString).flatMap(this::getSlot);
    }

    /* --- extra data collections --- */

    ConfigurationNode getExtraData(ItemStack stack, String key) throws UnreachableSlotDataException;

    ConfigurationNode getExtraData(ItemStackSnapshot snapshot, String key) throws UnreachableSlotDataException;

    void setExtraData(ItemStack stack, String key, ConfigurationNode data) throws UnreachableSlotDataException;

    default boolean hasExtraData(ItemStack stack, String key) throws UnreachableSlotDataException {
        return this.getExtraData(stack, key).getValue() != null;
    }

    default boolean hasExtraData(ItemStackSnapshot snapshot, String key) throws UnreachableSlotDataException {
        return this.getExtraData(snapshot, key).getValue() != null;
    }
}
