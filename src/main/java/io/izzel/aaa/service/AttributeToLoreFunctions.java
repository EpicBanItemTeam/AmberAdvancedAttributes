package io.izzel.aaa.service;

import com.flowpowered.math.GenericMath;
import com.google.common.collect.*;
import com.google.common.reflect.TypeToken;
import com.google.inject.Singleton;
import io.izzel.aaa.byteitems.ByteItemsHandler;
import io.izzel.aaa.collector.AttributeCollector;
import io.izzel.aaa.data.MarkerValue;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.data.StringValue;
import io.izzel.aaa.util.DataUtil;
import io.izzel.aaa.util.EquipmentUtil;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.amber.commons.i18n.args.Arg;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.izzel.aaa.command.AttributeCommands.LORE_SEPARATOR;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Singleton
public class AttributeToLoreFunctions {

    private static final DecimalFormat FORMAT = new DecimalFormat("+0.#;-0.#");
    private static final TypeToken<Text> TEXT = TypeToken.of(Text.class);

    public static <T extends RangeValue> AttributeToLoreFunction<T> rangeValue(AmberLocale locale, String id) {
        return (values, equipable) -> values.stream().map(it -> {
            String lower = it.isRelative() ? FORMAT.format(it.getLowerBound() * 100D) + "%" : FORMAT.format(it.getLowerBound());
            String higher = it.isRelative() ? FORMAT.format(it.getUpperBound() * 100D) + "%" : FORMAT.format(it.getUpperBound());
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
            String name = Sponge.getServer().getGameProfileManager().fill(it).join().getName().orElse("[Server]");
            Optional<Text> text = locale.getAs("attributes.possession.lore", TEXT, name);
            return Streams.stream(text);
        })).map(v -> Maps.immutableEntry(Byte.MIN_VALUE, v)).collect(ImmutableList.toImmutableList());
    }

    public static AttributeToLoreFunction<StringValue> equipment(AmberLocale locale) {
        return (values, equipable) -> {
            if (values.isEmpty()) {
                return ImmutableList.of();
            }
            Stream<Text> stream = values.stream().map(StringValue::getString)
                    .map(it -> locale.getAs("attributes.equipment.slots." + it, TEXT).orElse(Text.of(it)));
            Text joined = Text.joinWith(Text.of(' '), stream.iterator());
            Text ret = locale.getAs("attributes.equipment.item-name", TEXT, joined).get();
            return ImmutableList.of(Maps.immutableEntry((byte) 0, ret));
        };
    }

    public static AttributeToLoreFunction<StringValue> suit(AmberLocale locale, ByteItemsHandler biHandler) {
        return (values, equipable) -> {
            ImmutableList.Builder<Map.Entry<Byte, Text>> builder = ImmutableList.builder();

            values.forEach(it -> {
                ItemStackSnapshot suitItem = biHandler.read(it.getString());
                if (suitItem.equals(ItemStackSnapshot.NONE)) {
                    builder.add(Maps.immutableEntry(((byte) 16), locale.getAs("attributes.suit.unknown", TEXT, it.getString()).get()));
                } else {
                    builder.add(Maps.immutableEntry(((byte) 16), locale.getAs("attributes.suit.name", TEXT, suitItem.get(Keys.DISPLAY_NAME)).get()));
                    ItemStack stack = suitItem.createStack();
                    Map<EquipmentType, ItemStack> actualSlots = EquipmentUtil.itemsWithSlot(equipable)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    Attributes.EQUIPMENT.getValues(stack).stream()
                            .flatMap(eq -> Streams.stream(Sponge.getRegistry().getType(EquipmentType.class, eq.getString())))
                            .forEach(eq -> {
                                String node = actualSlots.containsKey(eq) && Attributes.SUIT.getValues(actualSlots.get(eq)).contains(it)
                                        ? "attributes.suit.present" : "attributes.suit.absent";
                                builder.add(Maps.immutableEntry(
                                        (byte) 16,
                                        locale.getAs(node, TEXT, Arg.ref("attributes.equipment.slots." + eq.getId())).get()
                                ));
                            });

                    builder.add(Maps.immutableEntry((byte) 24, locale.getAs("attributes.suit.suit-attribute-name", TEXT, suitItem.get(Keys.DISPLAY_NAME)).get()));
                    Text indent = locale.getAs("attributes.suit.attribute-indent", TEXT).get();
                    // todo 我觉得这下面一堆可以弄成一个方法
                    ListMultimap<Byte, Text> texts = Multimaps.newListMultimap(new TreeMap<>(), ArrayList::new);
                    Map<String, Attribute<?>> attributes = AttributeService.instance().getAttributes();
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
                ItemStackSnapshot snapshot = biHandler.read(it);
                if (snapshot.isEmpty()) {
                    builder.add(Maps.immutableEntry((byte) 0, locale.getAs("attributes.template.unknown", TEXT, it).get()));
                } else {
                    ListMultimap<Byte, Text> texts = Multimaps.newListMultimap(new TreeMap<>(), ArrayList::new);
                    for (Attribute<?> attribute : AttributeService.instance().getAttributes().values()) {
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

}
