package io.izzel.aaa.service;

import com.flowpowered.math.GenericMath;
import com.google.common.collect.*;
import com.google.common.reflect.TypeToken;
import com.google.inject.Singleton;
import io.izzel.aaa.byteitems.ByteItemsHandler;
import io.izzel.aaa.data.InlayData;
import io.izzel.aaa.data.MarkerValue;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.data.StringValue;
import io.izzel.aaa.util.DataUtil;
import io.izzel.aaa.util.EquipmentUtil;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.amber.commons.i18n.args.Arg;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TranslatableText;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static io.izzel.aaa.listener.AttributeListeners.LORE_SEPARATOR;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Singleton
public class AttributeToLoreFunctions {

    private static final DecimalFormat FORMAT = new DecimalFormat("+0.#;-0.#");
    private static final TypeToken<Text> TEXT = TypeToken.of(Text.class);

    public static <T extends RangeValue> AttributeToLoreFunction<T> rangeValue(AmberLocale locale, String id) {
        return (values, equipable) -> values.stream().map(it -> {
            var lower = it.isRelative() ? FORMAT.format(it.getLowerBound() * 100D) + "%" : FORMAT.format(it.getLowerBound());
            var higher = it.isRelative() ? FORMAT.format(it.getUpperBound() * 100D) + "%" : FORMAT.format(it.getUpperBound());
            if (it.getSize() < GenericMath.DBL_EPSILON) {
                return locale.getAs(String.format("attributes.%s.fixed", id), TEXT, lower).get();
            } else {
                return locale.getAs(String.format("attributes.%s.range", id), TEXT, lower, higher).get();
            }
        }).map(it -> Maps.immutableEntry((byte) 0, it)).collect(Collectors.toList());
    }

    public static AttributeToLoreFunction<MarkerValue> markerValue(AmberLocale locale, String id) {
        return (values, equipable) -> values.isEmpty() ? ImmutableList.of() : ImmutableList.of(Maps.immutableEntry((byte) 0,
                locale.getAs(String.format("attributes.%s.value", id), TEXT).get()));
    }

    public static AttributeToLoreFunction<RangeValue> durability(AmberLocale locale) {
        return (values, equipable) -> values.stream().map(it -> Maps.immutableEntry((byte) 64,
                locale.getAs("attributes.durability.value", TEXT,
                        (int) it.getLowerBound(), (int) it.getUpperBound()).get())).collect(Collectors.toList());
    }

    public static AttributeToLoreFunction<GameProfile> profile(AmberLocale locale) {
        return (values, equipable) -> (values.stream().flatMap(it -> {
            var name = Sponge.getServer().getGameProfileManager().fill(it).join().getName().orElse("[Server]");
            var text = locale.getAs("attributes.possession.lore", TEXT, name);
            return Streams.stream(text);
        })).map(v -> Maps.immutableEntry(Byte.MIN_VALUE, v)).collect(ImmutableList.toImmutableList());
    }

    public static AttributeToLoreFunction<StringValue> equipment(AmberLocale locale) {
        return (values, equipable) -> {
            if (values.isEmpty()) {
                return ImmutableList.of();
            }
            var stream = values.stream().map(StringValue::getString)
                    .map(it -> locale.getAs("attributes.equipment.slots." + it, TEXT).orElse(Text.of(it)));
            var joined = Text.joinWith(Text.of(' '), stream.iterator());
            var ret = locale.getAs("attributes.equipment.item-name", TEXT, joined).get();
            return ImmutableList.of(Maps.immutableEntry((byte) 0, ret));
        };
    }

    public static AttributeToLoreFunction<StringValue> suit(AmberLocale locale, ByteItemsHandler biHandler) {
        return (values, equipable) -> {
            ImmutableList.Builder<Map.Entry<Byte, Text>> builder = ImmutableList.builder();

            values.forEach(it -> {
                var suitItem = biHandler.read(it.getString());
                if (suitItem.equals(ItemStackSnapshot.NONE)) {
                    builder.add(Maps.immutableEntry(((byte) 16), locale.getAs("attributes.suit.unknown", TEXT, it.getString()).get()));
                } else {
                    builder.add(Maps.immutableEntry(((byte) 16), locale.getAs("attributes.suit.name", TEXT, suitItem.get(Keys.DISPLAY_NAME)).get()));
                    var stack = suitItem.createStack();
                    var actualSlots = EquipmentUtil.itemsWithSlot(equipable)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    Attributes.EQUIPMENT.getValues(stack).stream()
                            .map(StringValue::getString)
                            .forEach(slot -> {
                                var node = actualSlots.containsKey(slot) && Attributes.SUIT.getValues(actualSlots.get(slot)).contains(it)
                                        ? "attributes.suit.present" : "attributes.suit.absent";
                                builder.add(Maps.immutableEntry(
                                        (byte) 16,
                                        locale.getAs(node, TEXT, Arg.ref("attributes.equipment.slots." + slot)).get()
                                ));
                            });

                    builder.add(Maps.immutableEntry((byte) 24, locale.getAs("attributes.suit.suit-attribute-name", TEXT, suitItem.get(Keys.DISPLAY_NAME)).get()));
                    var indent = locale.getAs("attributes.suit.attribute-indent", TEXT).get();
                    // todo 我觉得这下面一堆可以弄成一个方法
                    ListMultimap<Byte, Text> texts = Multimaps.newListMultimap(new TreeMap<>(), ArrayList::new);
                    var attributes = AttributeService.instance().getAttributes();
                    attributes.values().stream().filter(attr -> attr != Attributes.EQUIPMENT)
                            .forEach(attribute -> DataUtil.collectLore(texts, stack, attribute, equipable));
                    Multimaps.asMap(texts).values().stream().reduce(ImmutableList.of(), (a, b) -> {
                        if (a.isEmpty()) {
                            return b;
                        } else {
                            return ImmutableList.<Text>builder().addAll(a).add(LORE_SEPARATOR).addAll(b).build();
                        }
                    }).forEach(text -> builder.add(Maps.immutableEntry((byte) 24, Text.of(indent, text))));
                }
            });

            return builder.build();
        };
    }

    public static AttributeToLoreFunction<StringValue> template(AmberLocale locale, ByteItemsHandler biHandler) {
        return (values, equipable) -> {
            ImmutableList.Builder<Map.Entry<Byte, Text>> builder = ImmutableList.builder();
            values.stream().map(StringValue::getString).filter(it -> !it.startsWith(";")).distinct().forEach(it -> {
                var snapshot = biHandler.read(it);
                if (snapshot.isEmpty()) {
                    builder.add(Maps.immutableEntry((byte) 0, locale.getAs("attributes.template.unknown", TEXT, it).get()));
                } else {
                    ListMultimap<Byte, Text> texts = Multimaps.newListMultimap(new TreeMap<>(), ArrayList::new);
                    for (var attribute : AttributeService.instance().getAttributes().values()) {
                        if (!Attributes.TEMPLATE.equals(attribute)) {
                            DataUtil.collectAllLore(texts, snapshot, attribute, equipable);
                        }
                    }
                    builder.addAll(texts.entries());
                }
            });
            return builder.build();
        };
    }

    public static AttributeToLoreFunction<InlayData> inlay(AmberLocale locale, ByteItemsHandler biHandler) {
        return (values, equipable) -> {
            if (values.isEmpty()) return ImmutableList.of();
            else {
                ImmutableList.Builder<Map.Entry<Byte, Text>> builder = ImmutableList.builder();
                builder.add(Maps.immutableEntry((byte) 0, locale.getAs("attributes.inlay.header", TEXT, values.size()).get()));
                values.forEach(data -> {
                    var slot = data.getSlot();
                    var t = locale.getAs("attributes.inlay.slot", TEXT,
                            Arg.ref("attributes.inlay.slot-names." + slot),
                            data.getGem().map(biHandler::read).map(item -> {
                                if (item.isEmpty())
                                    return locale.getAs("attributes.inlay.unknown", TEXT, data.getGem());
                                else
                                    return item.get(Keys.DISPLAY_NAME).orElse(TranslatableText.of(item.getTranslation()));
                            }).orElseGet(() -> Arg.ref("attributes.inlay.none"))
                    ).get();
                    builder.add(Maps.immutableEntry(((byte) 0), t));
                });
                return builder.build();
            }
        };
    }

    public static AttributeToLoreFunction<StringValue> permissionCap(AmberLocale locale) {
        return (values, equipable) -> values.stream()
                .map(StringValue::getString)
                .map(it -> locale.getAs("attributes.permission-cap.mappings." + it, TEXT).orElse(Text.of(it)))
                .map(it -> locale.getAs("attributes.permission-cap.value", TEXT, it))
                .flatMap(Streams::stream)
                .map(it -> Maps.immutableEntry((byte) 0, it))
                .collect(Collectors.toList());
    }

    public static AttributeToLoreFunction<PotionEffect> potionEffect(AmberLocale locale) {
        return (values, equipable) -> values.stream()
                .map(it -> locale.getAs("attributes.potion-effect.value", TEXT,
                        TranslatableText.of(it.getType().getTranslation()),
                        it.getAmplifier(),
                        BigDecimal.valueOf(it.getDuration() / 20D).setScale(2, RoundingMode.HALF_UP).toString()))
                .flatMap(Streams::stream)
                .map(it -> Maps.immutableEntry((byte) 0, it))
                .collect(Collectors.toList());
    }

}
