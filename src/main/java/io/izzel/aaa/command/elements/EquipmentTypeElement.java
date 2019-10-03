package io.izzel.aaa.command.elements;

import io.izzel.aaa.service.EquipmentSlotService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

@NonnullByDefault
public class EquipmentTypeElement extends CommandElement {

    public EquipmentTypeElement(String key) {
        super(Text.of(key));
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        var service = Sponge.getServiceManager().provideUnchecked(EquipmentSlotService.class);
        var next = args.next();
        if (service.slots().contains(next)) {
            return next;
        } else {
            throw args.createError(Text.of("Not a registered equipment slot"));
        }
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        var service = Sponge.getServiceManager().provideUnchecked(EquipmentSlotService.class);
        var all = args.getAll();
        return service.slots().stream().filter(it -> !all.contains(it))
                .collect(Collectors.toList());
    }
}
