package io.izzel.aaa.data;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.spongepowered.api.data.*;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

@NonnullByDefault
public class InlayData implements DataSerializable {

    private final String slot;
    @Nullable
    private String gem;

    private InlayData(String slot, @Nullable String gem) {
        Preconditions.checkNotNull(slot);
        this.slot = slot;
        this.gem = gem;
    }

    public static InlayData of(String slot, @Nullable String gem) {
        return new InlayData(slot, gem);
    }

    public static void register(DataManager dataManager) {
        dataManager.registerBuilder(InlayData.class, new Builder());
    }

    @Override
    public int getContentVersion() {
        return 0;
    }

    public String getSlot() {
        return slot;
    }

    public Optional<String> getGem() {
        return Optional.ofNullable(gem);
    }

    @Override
    public DataContainer toContainer() {
        var container = DataContainer.createNew()
                .set(Builder.SLOT_NAME, slot);
        if (gem != null) container = container.set(Builder.GEM, gem);
        return container.set(Queries.CONTENT_VERSION, this.getContentVersion());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var inlayData = (InlayData) o;
        return slot.equals(inlayData.slot) &&
                Objects.equals(gem, inlayData.gem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slot, gem);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("slot", slot)
                .add("gem", gem)
                .toString();
    }

    private static class Builder extends AbstractDataBuilder<InlayData> {

        private static final DataQuery SLOT_NAME = DataQuery.of("SlotName");
        private static final DataQuery GEM = DataQuery.of("Gem");

        Builder() {
            super(InlayData.class, 0);
        }

        @Override
        protected Optional<InlayData> buildContent(DataView container) throws InvalidDataException {
            var slot = container.getString(SLOT_NAME).orElseThrow(IllegalArgumentException::new);
            var gem = container.getString(GEM).orElse(null);
            return Optional.of(new InlayData(slot, gem));
        }
    }

}
