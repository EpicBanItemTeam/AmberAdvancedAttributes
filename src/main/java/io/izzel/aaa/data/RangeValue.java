package io.izzel.aaa.data;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.spongepowered.api.data.*;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;

@NonnullByDefault
public class RangeValue implements DataSerializable {
    private final boolean isRelative;
    private final double lowerBound;
    private final double upperBound;
    private final double diff;

    private RangeValue(double lower, double upper, boolean isRelative) {
        Preconditions.checkArgument(Double.isFinite(lower) && Double.isFinite(upper), "bounds should be finite");
        double diff = upper - lower;
        Preconditions.checkArgument(diff >= 0, "lower bound should be smaller than upper bound");
        this.isRelative = isRelative;
        this.lowerBound = lower;
        this.upperBound = upper;
        this.diff = diff;
    }

    public static RangeValue.Fixed absolute(double value) {
        return new RangeValue.Fixed(value, false);
    }

    public static RangeValue absolute(double lowerBound, double upperBound) {
        return new RangeValue(lowerBound, upperBound, false);
    }

    public static RangeValue.Fixed relative(double value) {
        return new RangeValue.Fixed(value, true);
    }

    public static RangeValue relative(double lowerBound, double upperBound) {
        return new RangeValue(lowerBound, upperBound, true);
    }

    public static DataBuilder<RangeValue> builder() {
        return new RangeValue.Builder();
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

    @Override
    public int hashCode() {
        return Objects.hash(this.isRelative, this.lowerBound, this.upperBound);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != this) {
            if (obj instanceof RangeValue) {
                RangeValue that = (RangeValue) obj;
                if (that.isRelative == this.isRelative) {
                    return that.lowerBound == this.lowerBound && that.upperBound == this.upperBound;
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("isRelative", isRelative)
                .add("lowerBound", lowerBound)
                .add("upperBound", upperBound)
                .toString();
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
                if (lowerBound == upperBound) {
                    return Optional.of(new RangeValue.Fixed(lowerBound, isRelative));
                } else {
                    return Optional.of(new RangeValue(lowerBound, upperBound, isRelative));
                }
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
    }

    @NonnullByDefault
    public static final class Fixed extends RangeValue {
        private Fixed(double value, boolean isRelative) {
            super(value, value, isRelative);
        }

        @Override
        public DoubleUnaryOperator getFunction(Random random) {
            return isRelative() ? d -> d * getLowerBound() : d -> getLowerBound();
        }
    }
}
