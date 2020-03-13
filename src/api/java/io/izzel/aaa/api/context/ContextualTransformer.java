package io.izzel.aaa.api.context;

import io.izzel.aaa.api.data.visitor.TemplatesVisitor;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Arrays;

@NonnullByDefault
@FunctionalInterface
public interface ContextualTransformer<T> {
    TemplatesVisitor transform(T context, TemplatesVisitor parentVisitor);

    static <T> ContextualTransformer<T> identity() {
        return (context, parentVisitor) -> parentVisitor;
    }

    static <T> ContextualTransformer<T> concat(ContextualTransformer<T> a, ContextualTransformer<T> b) {
        return (context, parentVisitor) -> a.transform(context, b.transform(context, parentVisitor));
    }

    static <T> ContextualTransformer<T> concat(ContextualTransformer<T>... transformers) {
        return Arrays.stream(transformers).reduce(identity(), ContextualTransformer::concat);
    }
}
