package io.izzel.aaa.data;

import org.spongepowered.api.data.*;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;

/**
 * Just a marker (singleton) which implements DataSerializable
 *
 * @author ustc_zzzz
 */
@NonnullByDefault
public final class MarkerValue implements DataSerializable {
    private final DataContainer container;

    private MarkerValue() {
        this.container = DataContainer.createNew().set(Queries.CONTENT_VERSION, this.getContentVersion());
    }

    public static MarkerValue of() {
        return Builder.INSTANCE;
    }

    public static void register(DataManager dataManager) {
        dataManager.registerBuilder(MarkerValue.class, new MarkerValue.Builder());
    }

    @Override
    public int getContentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        return this.container.copy();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @NonnullByDefault
    private static class Builder extends AbstractDataBuilder<MarkerValue> {
        private static final MarkerValue INSTANCE = new MarkerValue();

        private Builder() {
            super(MarkerValue.class, 0);
        }

        @Override
        protected Optional<MarkerValue> buildContent(DataView container) throws InvalidDataException {
            return Optional.of(INSTANCE);
        }
    }
}
