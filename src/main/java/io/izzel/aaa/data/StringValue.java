package io.izzel.aaa.data;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.spongepowered.api.data.*;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Objects;
import java.util.Optional;

@NonnullByDefault
public final class StringValue implements DataSerializable {

    private final String string;

    private StringValue(String string) {
        Preconditions.checkNotNull(string);
        this.string = string;
    }

    public static StringValue of(String str) {
        return new StringValue(str);
    }

    public static void register(DataManager dataManager) {
        dataManager.registerBuilder(StringValue.class, new Builder());
    }

    @Override
    public int getContentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        return DataContainer.createNew()
                .set(Builder.STRING, string)
                .set(Queries.CONTENT_VERSION, this.getContentVersion());
    }

    public String getString() {
        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (StringValue) o;
        return string.equals(that.string);
    }

    @Override
    public int hashCode() {
        return Objects.hash(string);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("string", string)
                .toString();
    }

    private static class Builder extends AbstractDataBuilder<StringValue> {

        private static final DataQuery STRING = DataQuery.of("String");

        private Builder() {
            super(StringValue.class, 0);
        }

        @Override
        protected Optional<StringValue> buildContent(DataView container) throws InvalidDataException {
            return container.getString(STRING).map(StringValue::new);
        }
    }

}
