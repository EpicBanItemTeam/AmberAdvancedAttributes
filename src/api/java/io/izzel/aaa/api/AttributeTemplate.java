package io.izzel.aaa.api;

import com.google.common.base.Preconditions;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.function.Predicate;
import java.util.regex.Pattern;

@NonnullByDefault
public final class AttributeTemplate implements Comparable<AttributeTemplate> {
    private static final Predicate<String> PREDICATE = Pattern.compile("[a-z0-9_-]+").asPredicate();

    private final String templateString;

    private AttributeTemplate(String templateString) {
        Preconditions.checkArgument(PREDICATE.test(templateString), "invalid template string");
        this.templateString = templateString;
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
        return this == o || o instanceof AttributeTemplate && ((AttributeTemplate) o).templateString.equals(this.templateString);
    }

    @Override
    public int compareTo(AttributeTemplate that) {
        return this.templateString.compareTo(that.templateString);
    }
}
