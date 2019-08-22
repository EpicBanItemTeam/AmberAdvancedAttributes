package io.izzel.aaa.command;

import io.izzel.aaa.util.EquipmentUtil;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

@NonnullByDefault
public class EquipmentTypeElement extends CommandElement {

    EquipmentTypeElement(String key) {
        super(Text.of(key));
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        return Sponge.getRegistry().getType(EquipmentType.class, args.next())
                .orElseThrow(() -> args.createError(Text.of("Not a equipment type")));
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        List<String> all = args.getAll();
        return EquipmentUtil.EQUIPMENT_TYPES.stream().map(CatalogType::getId).filter(it -> !all.contains(it))
                .collect(Collectors.toList());
    }
}
