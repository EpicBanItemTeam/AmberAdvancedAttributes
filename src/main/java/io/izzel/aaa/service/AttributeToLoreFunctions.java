package io.izzel.aaa.service;

import com.flowpowered.math.GenericMath;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.reflect.TypeToken;
import io.izzel.aaa.data.MarkerValue;
import io.izzel.aaa.data.RangeValue;
import io.izzel.amber.commons.i18n.AmberLocale;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;

import java.text.DecimalFormat;
import java.util.Optional;
import java.util.stream.Collectors;

public class AttributeToLoreFunctions {

    private static final DecimalFormat FORMAT = new DecimalFormat("+0.#;-0.#");

    public static <T extends RangeValue> AttributeToLoreFunction<T> rangeValue(AmberLocale locale, String id) {
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
        }).map(it -> Maps.immutableEntry((byte) 0, it)).collect(Collectors.toList());
    }

    public static AttributeToLoreFunction<MarkerValue> markerValue(AmberLocale locale, String id) {
        return values -> values.isEmpty() ? ImmutableList.of() : ImmutableList.of(Maps.immutableEntry((byte) 0,
                locale.getAs(String.format("attributes.%s.value", id), TypeToken.of(Text.class)).orElseThrow(RuntimeException::new)));
    }

    public static AttributeToLoreFunction<RangeValue> durability(AmberLocale locale) {
        return values -> values.stream().map(it -> Maps.immutableEntry((byte) 0,
                locale.getAs("attributes.durability.value", TypeToken.of(Text.class),
                        (int) it.getLowerBound(), (int) it.getUpperBound()).orElseThrow(RuntimeException::new))).collect(Collectors.toList());
    }

    public static AttributeToLoreFunction<GameProfile> profile(AmberLocale locale) {
        return values -> (values.stream().flatMap(it -> {
            String name = Sponge.getServer().getGameProfileManager().fill(it).join().getName().orElse("[Server]");
            Optional<Text> text = locale.getAs("attributes.possession.lore", TypeToken.of(Text.class), name);
            return Streams.stream(text);
        })).map(v -> Maps.immutableEntry(Byte.MIN_VALUE, v)).collect(ImmutableList.toImmutableList());
    }
}
