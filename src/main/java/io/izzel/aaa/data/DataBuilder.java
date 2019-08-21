package io.izzel.aaa.data;

import com.google.common.collect.ImmutableListMultimap;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class DataBuilder extends AbstractDataBuilder<Data> implements DataManipulatorBuilder<Data, ImmutableData> {
    DataBuilder() {
        super(Data.class, 0);
    }

    @Override
    public Data create() {
        return new Data(ImmutableListMultimap.of());
    }

    @Override
    public Optional<Data> createFrom(DataHolder dataHolder) {
        return this.create().fill(dataHolder);
    }

    @Override
    protected Optional<Data> buildContent(DataView container) {
        return Optional.of(Data.fromContainer(container, this.create()));
    }
}
