package io.izzel.aaa.data;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData;
import org.spongepowered.api.util.annotation.NonnullByDefault;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class ImmutableData extends AbstractImmutableData<ImmutableData, Data> {
    private final ImmutableListMultimap<String, Object> data;

    ImmutableData(ListMultimap<String, ?> data) {
        this.data = ImmutableListMultimap.copyOf(data);
    }

    @Override
    protected void registerGetters() {
        // register nothing
    }

    @Override
    public Data asMutable() {
        return new Data(this.data);
    }

    @Override
    public int getContentVersion() {
        return 0;
    }

    @Override
    protected DataContainer fillContainer(DataContainer dataContainer) {
        return Data.fillContainer(super.fillContainer(dataContainer), this.data);
    }
}
