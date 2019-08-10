package io.izzel.aaa.data;

import com.google.common.base.Preconditions;
import org.spongepowered.api.data.*;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;

@NonnullByDefault
public final class RangeValue implements DataSerializable {
    private final boolean isRelative;
    private final double lowerBound;
    private final double upperBound;
    private final double diff;

    public RangeValue(double lower, double upper, boolean isRelative) {
        Preconditions.checkArgument(Double.isFinite(lower) && Double.isFinite(upper), "bounds should be finite");
        double diff = upper - lower;
        Preconditions.checkArgument(diff >= 0, "lower bound should be smaller than upper bound");
        this.isRelative = isRelative;
        this.lowerBound = lower;
        this.upperBound = upper;
        this.diff = diff;
    }

    public DoubleUnaryOperator getFunction(Random random) {
        if (random.nextBoolean()) {
            double amount = this.lowerBound + this.diff * random.nextDouble();
            return this.isRelative ? d -> d * amount : d -> amount;
        } else {
            double amount = this.upperBound - this.diff * random.nextDouble();
            return this.isRelative ? d -> d * amount : d -> amount;
        }
    }

    public boolean isRelative() {
        return this.isRelative;
    }

    public double getLowerBound() {
        return this.lowerBound;
    }

    public double getUpperBound() {
        return this.upperBound;
    }

    public double getSize() {
        return this.diff;
    }

    @Override
    public int getContentVersion() {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        return DataContainer.createNew()
                .set(Builder.RELATIVE, this.isRelative)
                .set(Builder.UPPER_BOUND, this.upperBound)
                .set(Builder.LOWER_BOUND, this.lowerBound)
                .set(Queries.CONTENT_VERSION, this.getContentVersion());
    }

    public static RangeValue absolute(double value) {
        return new RangeValue(value, value, false);
    }

    public static RangeValue absolute(double lowerBound, double upperBound) {
        return new RangeValue(lowerBound, upperBound, false);
    }

    public static RangeValue relative(double value) {
        return new RangeValue(value, value, true);
    }

    public static RangeValue relative(double lowerBound, double upperBound) {
        return new RangeValue(lowerBound, upperBound, true);
    }

    public static DataBuilder<RangeValue> builder() {
        return new RangeValue.Builder();
    }

    @NonnullByDefault
    private static class Builder extends AbstractDataBuilder<RangeValue> {
        private static final DataQuery RELATIVE = DataQuery.of("Relative");
        private static final DataQuery LOWER_BOUND = DataQuery.of("LowerBound");
        private static final DataQuery UPPER_BOUND = DataQuery.of("UpperBound");

        private Builder() {
            super(RangeValue.class, 0);
        }

        @Override
        protected Optional<RangeValue> buildContent(DataView container) throws InvalidDataException {
            try {
                double lowerBound = container.getDouble(LOWER_BOUND).orElseThrow(IllegalArgumentException::new);
                double upperBound = container.getDouble(UPPER_BOUND).orElseThrow(IllegalArgumentException::new);
                boolean isRelative = container.getBoolean(RELATIVE).orElse(Boolean.FALSE);
                return Optional.of(new RangeValue(lowerBound, upperBound, isRelative));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
    }
}
