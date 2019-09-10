package io.izzel.aaa.command.elements;

import com.google.common.collect.ImmutableList;
import io.izzel.aaa.data.InlayData;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.annotation.Nullable;
import java.util.List;

@NonnullByDefault
public class InlayDataElement extends CommandElement {

    public InlayDataElement(String key) {
        super(Text.of(key));
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        return InlayData.of(args.next(), args.nextIfPresent().orElse(null));
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        return ImmutableList.of();
    }
}
