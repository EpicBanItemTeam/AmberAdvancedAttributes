package io.izzel.aaa.data;

import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Random;
import java.util.function.DoubleUnaryOperator;

@NonnullByDefault
public final class FixedValue extends RangeValue {

    public FixedValue(double value, boolean isRelative) {
        super(value, value, isRelative);
    }

    @Override
    public DoubleUnaryOperator getFunction(Random random) {
        return isRelative() ? d -> d * getLowerBound() : d -> getLowerBound();
    }

    @Override
    public double getSize() {
        return 0D;
    }

}
