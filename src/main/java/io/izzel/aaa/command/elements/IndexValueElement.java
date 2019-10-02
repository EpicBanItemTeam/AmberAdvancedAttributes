package io.izzel.aaa.command.elements;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import io.izzel.amber.commons.i18n.AmberLocale;
import org.apache.commons.lang3.Validate;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * values can be: 1, -1
 *
 * @author yinyangshi ustc_zzzz
 */
@NonnullByDefault
public class IndexValueElement extends CommandElement {
    private static final ImmutableList<String> AT_LIST = ImmutableList.of("at", "into", "->");
    private final TypeToken<Text> token;
    private final AmberLocale locale;

    public IndexValueElement(AmberLocale locale, Text key) {
        super(key);
        this.locale = locale;
        this.token = TypeToken.of(Text.class);
    }

    @Nullable
    @Override
    protected Integer parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        var string = args.next();
        try {
            Validate.isTrue(AT_LIST.contains(string));
            Validate.isTrue(args.hasNext());
            return Integer.parseInt(args.next());
        } catch (Exception e) {
            var text = this.locale.getAs("commands.args.not-a-number", this.token, string);
            throw args.createError(text.orElseThrow(IllegalStateException::new));
        }
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        var snapshot = args.getSnapshot();
        if (args.nextIfPresent().isPresent()) {
            var literal = args.nextIfPresent();
            if (literal.isPresent() && !args.hasNext()) {
                args.applySnapshot(snapshot);
                return AT_LIST;
            }
        }
        args.applySnapshot(snapshot);
        return ImmutableList.of();
    }
}

