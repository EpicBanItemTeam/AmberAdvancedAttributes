package io.izzel.aaa.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.izzel.aaa.AmberAdvancedAttributes;
import io.izzel.aaa.command.elements.EquipmentTypeElement;
import io.izzel.aaa.data.StringValue;
import io.izzel.aaa.service.Attribute;
import io.izzel.amber.commons.i18n.AmberLocale;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

class EquipmentCommand {

    @Inject private AmberLocale locale;
    @Inject private ValueCommand command;

    CommandCallable callable(Attribute<StringValue> attribute) {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-equipment")
                .arguments(
                        GenericArguments.choices(Text.of("marked"),
                                ImmutableMap.of("mark", Boolean.TRUE, "unmark", Boolean.FALSE)),
                        GenericArguments.allOf(new EquipmentTypeElement("slots"))
                )
                .child(command.clear("equipment", attribute), "clear")
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        var player = (Player) src;
                        var optional = player.getItemInHand(HandTypes.MAIN_HAND);
                        if (optional.isPresent()) {
                            var stack = optional.get();
                            var old = attribute.getValues(stack);
                            boolean mark = args.<Boolean>getOne("marked").orElse(Boolean.FALSE);
                            Collection<StringValue> slots = args.<String>getAll("slots").stream()
                                    .map(StringValue::of)
                                    .collect(Collectors.toList());
                            var list = mark
                                    ? ImmutableList.<StringValue>builder().addAll(old).addAll(slots).build()
                                    : ImmutableList.copyOf(old.stream().filter(it -> !slots.contains(it)).iterator());
                            if (list.isEmpty()) {
                                attribute.clearValues(stack);
                            } else {
                                attribute.setValues(stack, list);
                            }
                            this.locale.to(src, "commands.marker.mark-attribute", stack, "equipment");
                        }
                        return CommandResult.success();
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

}
