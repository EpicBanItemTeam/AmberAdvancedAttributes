package io.izzel.aaa.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.AttributeService;
import org.spongepowered.api.data.*;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.*;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class Data extends AbstractData<Data, ImmutableData> {
    private final ListMultimap<String, Object> data;

    Data(ListMultimap<String, ?> data) {
        this.data = Multimaps.newListMultimap(new LinkedHashMap<>(), ArrayList::new);
        this.data.putAll(data);
    }

    public <T extends DataSerializable> List<? extends T> get(Attribute<T> attribute) {
        @SuppressWarnings("unchecked")
        List<T> result = (List) this.data.get(attribute.getId());
        return result;
    }

    public <T extends DataSerializable> Data set(Attribute<T> attribute, List<? extends T> value) {
        this.data.replaceValues(attribute.getId(), value);
        return this;
    }

    @Override
    protected void registerGettersAndSetters() {
        // register nothing
    }

    @Override
    public Optional<Data> fill(DataHolder dataHolder, MergeFunction overlap) {
        Data original = dataHolder.get(Data.class).orElse(null);
        Data data = overlap.merge(this, original);
        for (String id : AttributeService.instance().getAttributes().keySet()) {
            this.data.replaceValues(id, data.data.get(id));
        }
        return Optional.of(this);
    }

    @Override
    public Optional<Data> from(DataContainer container) {
        try {
            return this.fromContainer(container);
        } catch (InvalidDataException e) {
            return Optional.empty();
        }
    }

    @Override
    public Data copy() {
        return new Data(this.data);
    }

    @Override
    public ImmutableData asImmutable() {
        return new ImmutableData(this.data);
    }

    @Override
    public int getContentVersion() {
        return 0;
    }

    @Override
    protected DataContainer fillContainer(DataContainer dataContainer) {
        DataContainer container = super.fillContainer(dataContainer);
        for (Map.Entry<String, Attribute<?>> entry : AttributeService.instance().getAttributes().entrySet()) {
            List<Object> values = this.data.get(entry.getKey());
            if (!values.isEmpty()) {
                container.set(DataQuery.of(entry.getKey()), values);
            }
        }
        return container;
    }

    private Optional<Data> fromContainer(DataView container) {
        for (Map.Entry<String, Attribute<?>> entry : AttributeService.instance().getAttributes().entrySet()) {
            String key = entry.getKey();
            DataQuery query = DataQuery.of(key);
            Class<? extends DataSerializable> clazz = entry.getValue().getDataClass();
            this.data.replaceValues(key, container.getSerializableList(query, clazz).orElse(ImmutableList.of()));
        }
        return Optional.of(this);
    }

    @NonnullByDefault
    public static class Builder extends AbstractDataBuilder<Data> implements DataManipulatorBuilder<Data, ImmutableData> {
        public Builder() {
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
            return this.create().fromContainer(container);
        }
    }
}
