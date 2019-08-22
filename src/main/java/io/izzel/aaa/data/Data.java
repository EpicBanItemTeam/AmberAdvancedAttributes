package io.izzel.aaa.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import io.izzel.aaa.AmberAdvancedAttributes;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.AttributeService;
import org.spongepowered.api.data.*;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
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

    public static void register(DataManager dataManager) {
        Objects.requireNonNull(dataManager);
        DataRegistration.builder()
                .dataClass(Data.class)
                .builder(new DataBuilder())
                .immutableClass(ImmutableData.class)
                .id("data").name(AmberAdvancedAttributes.NAME).build();
    }

    public <T extends DataSerializable> ImmutableList<T> get(Attribute<T> attribute) {
        List<?> values = this.data.get(attribute.getId());
        return values.stream().map(attribute.getDataClass()::cast).collect(ImmutableList.toImmutableList());
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
            return Optional.of(fromContainer(container, this));
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
        return fillContainer(super.fillContainer(dataContainer), this.data);
    }

    static DataContainer fillContainer(DataContainer container, ListMultimap<String, ?> data) {
        for (Map.Entry<String, Attribute<?>> entry : AttributeService.instance().getAttributes().entrySet()) {
            String key = entry.getKey();
            List<?> values = data.get(key);
            if (values.isEmpty()) {
                DataQuery query = DataQuery.of(key);
                container.remove(query);
            } else {
                Class<? extends DataSerializable> clazz = entry.getValue().getDataClass();
                if (MarkerValue.class == clazz) {
                    DataQuery query = DataQuery.of(key);
                    container.set(query, values.size());
                } else {
                    DataQuery query = DataQuery.of(key);
                    container.set(query, values);
                }
            }
        }
        return container;
    }

    static Data fromContainer(DataView container, Data data) {
        for (Map.Entry<String, Attribute<?>> entry : AttributeService.instance().getAttributes().entrySet()) {
            String key = entry.getKey();
            DataQuery query = DataQuery.of(key);
            if (container.contains(query)) {
                Class<? extends DataSerializable> clazz = entry.getValue().getDataClass();
                if (MarkerValue.class == clazz) {
                    List<?> values = Collections.nCopies(container.getInt(query).orElse(0), MarkerValue.of());
                    data.data.replaceValues(key, values);
                } else {
                    List<?> values = container.getSerializableList(query, clazz).orElse(ImmutableList.of());
                    data.data.replaceValues(key, values);
                }
            } else {
                data.data.removeAll(key);
            }
        }
        return data;
    }
}
