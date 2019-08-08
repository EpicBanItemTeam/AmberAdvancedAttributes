package io.izzel.aaa.service;

import com.flowpowered.math.GenericMath;
import com.google.common.reflect.TypeToken;
import io.izzel.aaa.Main;
import io.izzel.aaa.data.RangeValue;
import io.izzel.amber.commons.i18n.AmberLocale;
import org.spongepowered.api.text.Text;

import java.text.DecimalFormat;
import java.util.AbstractMap;
import java.util.stream.Collectors;

public class AttributeToLoreFunctions {

    private static final DecimalFormat FORMAT = new DecimalFormat("+0.#;-0.#");

    public static AttributeToLoreFunction<RangeValue> rangeValue(String id) {
        AmberLocale locale = Main.INSTANCE.locale;
        return values -> values.stream().map(it -> {
            String lower = it.isRelative() ? FORMAT.format(it.getLowerBound() * 100D) + "%" : FORMAT.format(it.getLowerBound());
            String higher = it.isRelative() ? FORMAT.format(it.getUpperBound() * 100D) + "%" : FORMAT.format(it.getUpperBound());
            if (it.getSize() < GenericMath.DBL_EPSILON) {
                return locale.getAs(String.format("attributes.%s.fixed", id), TypeToken.of(Text.class), lower)
                    .orElseThrow(RuntimeException::new);
            } else {
                return locale.getAs(String.format("attributes.%s.range", id), TypeToken.of(Text.class), lower, higher)
                    .orElseThrow(RuntimeException::new);
            }
        }).map(it -> new AbstractMap.SimpleEntry<>((byte) 0, it)).collect(Collectors.toList());
    }

}
