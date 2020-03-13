package io.izzel.aaa.api.data;

import com.google.common.base.Preconditions;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.function.Predicate;
import java.util.regex.Pattern;

@NonnullByDefault
public final class Template implements Comparable<Template> {
    private static final Predicate<String> PREDICATE = Pattern.compile("[a-z0-9_-]+").asPredicate();

    private final String templateString;

    private Template(String templateString) {
        Preconditions.checkArgument(PREDICATE.test(templateString), "invalid template string");
        this.templateString = templateString;
    }

    public static Template parse(String templateString) {
        Preconditions.checkNotNull(templateString);
        return new Template(templateString);
    }

    @Override
    public String toString() {
        return this.templateString;
    }

    @Override
    public int hashCode() {
        return templateString.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Template && ((Template) o).templateString.equals(this.templateString);
    }

    @Override
    public int compareTo(Template that) {
        return this.templateString.compareTo(that.templateString);
    }
}
