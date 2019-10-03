package io.izzel.aaa.command.elements;

import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Coerce;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@NonnullByDefault
public class PotionEffectElement extends CommandElement {

    public PotionEffectElement(String key) {
        super(Text.of(key));
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        Optional<PotionEffectType> type;
        if (args.hasNext()) type = Sponge.getRegistry().getType(PotionEffectType.class, args.next());
        else throw args.createError(Text.of("No PotionEffectType present"));
        if (!type.isPresent()) throw args.createError(Text.of("No valid PotionEffectType present"));
        Optional<Integer> duration;
        if (args.hasNext()) duration = Coerce.asInteger(args.next());
        else duration = Optional.empty();
        if (!duration.isPresent()) throw args.createError(Text.of("No valid duration present"));
        int amplifier;
        if (args.hasNext()) {
            var integer = Coerce.asInteger(args.peek());
            if (integer.isPresent()) {
                amplifier = integer.get();
                args.next();
            } else amplifier = 1;
        } else amplifier = 1;
        return PotionEffect.builder().potionType(type.get()).amplifier(amplifier)
                .duration(duration.get()).build();
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        return Sponge.getRegistry().getAllOf(PotionEffectType.class).stream().map(CatalogType::getId)
                .collect(Collectors.toList());
    }

    @Override
    public Text getUsage(CommandSource src) {
        return Text.of("<type> <duration> [amplifier]");
    }

}
