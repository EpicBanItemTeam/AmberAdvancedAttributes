package io.izzel.aaa.command;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.izzel.aaa.AmberAdvancedAttributes;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.util.DataUtil;
import io.izzel.amber.commons.i18n.AmberLocale;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;

class PossessCommand {

    @Inject private AmberLocale locale;

    CommandCallable possess(Attribute<GameProfile> attribute) {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-possess")
                .arguments(GenericArguments.optional(GenericArguments.player(Text.of("player"))))
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        var stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        var target = args.<Player>getOne(Text.of("player")).orElse((Player) src);
                        if (stackOptional.isPresent()) {
                            var stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.setValues(stack, ImmutableList.of(target.getProfile()));
                                this.locale.to(src, "commands.possess.mark-attribute", target.getName());
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    CommandCallable publicize(Attribute<GameProfile> attribute) {
        return CommandSpec.builder()
                .permission(AmberAdvancedAttributes.ID + ".command.aaa-publicize")
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        var stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        if (stackOptional.isPresent()) {
                            var stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.clearValues(stack);
                                this.locale.to(src, "commands.possess.unmark-attribute");
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }
}
