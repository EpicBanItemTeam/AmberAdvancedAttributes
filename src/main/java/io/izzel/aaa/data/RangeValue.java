package io.izzel.aaa.data;

import org.spongepowered.api.data.DataSerializable;

import java.util.function.DoubleUnaryOperator;

public interface RangeValue extends DataSerializable {

    DoubleUnaryOperator getFunction();

}
