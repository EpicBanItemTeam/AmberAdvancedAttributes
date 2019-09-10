package io.izzel.aaa.command;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.izzel.aaa.AmberAdvancedAttributes;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.util.DataUtil;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.amber.commons.i18n.args.Arg;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

class InitDropCommand {

    @Inject private AmberLocale locale;

    CommandCallable init(Attribute<Text> attribute) {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-init")
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        if (stackOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                this.locale.to(src, "commands.init.already-exist");
                            } else {
                                attribute.setValues(stack, stack.get(Keys.ITEM_LORE).orElse(ImmutableList.of()));
                                ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                this.locale.to(src, "commands.init.succeed");
                            }
                            return CommandResult.success();
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    CommandCallable drop(Attribute<Text> attribute) {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-drop")
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        AtomicBoolean isCallbackExecuted = new AtomicBoolean(false);
                        Arg arg = Arg.ref("commands.drop.warning-ok").withCallback(value -> {
                            if (!isCallbackExecuted.getAndSet(true)) {
                                Optional<ItemStack> stackOptional = ((Player) value).getItemInHand(HandTypes.MAIN_HAND);
                                if (stackOptional.isPresent()) {
                                    ItemStack stack = stackOptional.get();
                                    if (DataUtil.hasData(stack)) {
                                        List<Text> lore = attribute.getValues(stack);
                                        DataUtil.dropData(stack);
                                        stack.offer(Keys.ITEM_LORE, lore);
                                        ((Player) value).setItemInHand(HandTypes.MAIN_HAND, stack);
                                        this.locale.to(value, "commands.drop.succeed");
                                        return;
                                    }
                                }
                                this.locale.to(value, "commands.drop.nonexist");
                            }
                        });
                        locale.to(src, "commands.drop.warning", arg);
                        return CommandResult.success();
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }
}
