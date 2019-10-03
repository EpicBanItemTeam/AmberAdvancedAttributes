package io.izzel.aaa.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.izzel.aaa.AmberAdvancedAttributes;
import io.izzel.aaa.command.elements.IndexValueElement;
import io.izzel.aaa.data.MarkerValue;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.Attributes;
import io.izzel.aaa.util.DataUtil;
import io.izzel.amber.commons.i18n.AmberLocale;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@SuppressWarnings("WeakerAccess")
class ValueCommand {

    @Inject private AmberLocale locale;

    CommandCallable noLore(String id, Attribute<MarkerValue> attribute) {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-" + id)
                .arguments(GenericArguments.choices(Text.of("marked"),
                        ImmutableMap.of("mark", Boolean.TRUE, "unmark", Boolean.FALSE)))
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        var stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Optional<Boolean> marked = args.getOne(Text.of("marked"));
                        if (stackOptional.isPresent() && marked.isPresent()) {
                            var stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                if (marked.get()) {
                                    attribute.setValues(stack, ImmutableList.of(MarkerValue.of()));
                                    var list = Attributes.ORIGINAL_LORE.getValues(stack);
                                    stack.offer(Keys.ITEM_LORE, list);
                                    Attributes.ORIGINAL_LORE.clearValues(stack);
                                    ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                    this.locale.to(src, "commands.marker.mark-attribute", stack, id);
                                } else {
                                    attribute.clearValues(stack);
                                    var list = stack.get(Keys.ITEM_LORE).orElse(ImmutableList.of());
                                    Attributes.ORIGINAL_LORE.setValues(stack, list);
                                    ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                    this.locale.to(src, "commands.marker.unmark-attribute", stack, id);
                                }
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    CommandCallable marker(String id, Attribute<MarkerValue> attribute) {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-" + id)
                .arguments(GenericArguments.choices(Text.of("marked"),
                        ImmutableMap.of("mark", Boolean.TRUE, "unmark", Boolean.FALSE)))
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        var stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Optional<Boolean> marked = args.getOne(Text.of("marked"));
                        if (stackOptional.isPresent() && marked.isPresent()) {
                            var stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                if (marked.get()) {
                                    attribute.setValues(stack, ImmutableList.of(MarkerValue.of()));
                                    ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                    this.locale.to(src, "commands.marker.mark-attribute", stack, id);
                                } else {
                                    attribute.clearValues(stack);
                                    ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                    this.locale.to(src, "commands.marker.unmark-attribute", stack, id);
                                }
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    <T extends DataSerializable> CommandCallable prepend(String id, Attribute<T> attribute, CommandElement valueElement) {
        return CommandSpec.builder()
                .arguments(valueElement)
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        var stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Optional<T> rangeValueOptional = args.getOne(valueElement.getKey());
                        if (stackOptional.isPresent() && rangeValueOptional.isPresent()) {
                            var stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.prependValue(stack, rangeValueOptional.get());
                                ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                this.locale.to(src, "commands.range.prepend-attribute", stack, id);
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    /**
     * get the append command for a attribute value to sth.
     *
     * @param id           the id of the attribute
     * @param attribute    the attribute value
     * @param valueElement the command value element
     * @param <T>          the type of the value
     * @return the command to append the value to the item stack.
     */
    <T extends DataSerializable> CommandCallable append(String id, Attribute<T> attribute, CommandElement valueElement) {
        return CommandSpec.builder()
                .arguments(valueElement)
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        var stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Optional<T> rangeValueOptional = args.getOne(valueElement.getKey());
                        if (stackOptional.isPresent() && rangeValueOptional.isPresent()) {
                            var stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.appendValue(stack, rangeValueOptional.get());
                                ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                this.locale.to(src, "commands.range.append-attribute", stack, id);
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    /**
     * get the insert command for a attribute value to sth.
     *
     * @param id           the id of the attribute
     * @param attribute    the attribute
     * @param valueElement the command value element
     * @param <T>          the type of the value
     * @return the command to insert the value
     */
    <T extends DataSerializable> CommandCallable insert(String id, Attribute<T> attribute, CommandElement valueElement) {
        return CommandSpec.builder()
                .arguments(valueElement, new IndexValueElement(this.locale, Text.of("index")))
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        int index = args.<Integer>getOne(Text.of("index")).orElseThrow(NoSuchElementException::new);
                        var stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Optional<T> rangeValueOptional = args.getOne(valueElement.getKey());
                        if (stackOptional.isPresent() && rangeValueOptional.isPresent()) {
                            var stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.insertValue(stack, index, rangeValueOptional.get());
                                ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                this.locale.to(src, "commands.range.append-attribute", stack, id);
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    <T extends DataSerializable> CommandCallable clear(String id, Attribute<T> attribute) {
        return CommandSpec.builder()
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        var stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        if (stackOptional.isPresent()) {
                            var stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.clearValues(stack);
                                ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                this.locale.to(src, "commands.range.clear-attribute", stack, id);
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    <T extends DataSerializable> CommandCallable set(String id, Attribute<T> attribute, CommandElement valueElement) {
        return CommandSpec.builder()
                .arguments(valueElement, GenericArguments.optionalWeak(new IndexValueElement(this.locale, Text.of("index"))))
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        var index = args.<Integer>getOne(Text.of("index")).orElse(1) - 1;
                        var stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Optional<T> rangeValueOptional = args.getOne(valueElement.getKey());
                        if (stackOptional.isPresent() && rangeValueOptional.isPresent()) {
                            var stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                List<T> list = Lists.newArrayList(attribute.getValues(stack));
                                if (index >= list.size() || index < 0) {
                                    this.locale.to(src, "commands.args.index-out-of-bound", list.size());
                                    return CommandResult.success();
                                }
                                list.set(index, rangeValueOptional.get());
                                attribute.setValues(stack, list);
                                ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                this.locale.to(src, "commands.range.set-attribute", stack, index, id);
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    <T extends DataSerializable> CommandCallable callable(Attribute<T> attribute, String id, CommandElement element) {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-" + id)
                .child(append(id, attribute, element), "append")
                .child(prepend(id, attribute, element), "prepend")
                .child(clear(id, attribute), "clear")
                .child(insert(id, attribute, element), "insert")
                .child(set(id, attribute, element), "set")
                .build();
    }
}
