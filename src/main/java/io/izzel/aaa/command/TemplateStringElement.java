package io.izzel.aaa.command;

import com.google.common.collect.ImmutableList;
import io.izzel.aaa.data.StringValue;
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
public class TemplateStringElement extends CommandElement {

    protected TemplateStringElement(@Nullable Text key) {
        super(key);
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        String next = args.next();
        if (args.hasNext() && args.peek().equals("as")) {
            args.next();
            if (args.next().equals("hidden")) {
                return StringValue.of(";" + next);
            }
        }
        return StringValue.of(next);
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        return ImmutableList.of();
    }
}
