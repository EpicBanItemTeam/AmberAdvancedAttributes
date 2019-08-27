package io.izzel.aaa.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import io.izzel.aaa.service.Attribute;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class ImmutableData extends AbstractImmutableData<ImmutableData, Data> {
    private final ImmutableListMultimap<String, Object> data;

    ImmutableData(ListMultimap<String, ?> data) {
        this.data = ImmutableListMultimap.copyOf(data);
    }

    public <T extends DataSerializable> ImmutableList<T> get(Attribute<T> attribute) {
        List<?> values = this.data.get(attribute.getId());
        return values.stream().map(attribute.getDataClass()::cast).collect(ImmutableList.toImmutableList());
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
