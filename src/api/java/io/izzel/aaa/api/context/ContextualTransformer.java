package io.izzel.aaa.api.context;

import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Arrays;

@NonnullByDefault
@FunctionalInterface
public interface ContextualTransformer<T, U> {
    U transform(T context, U parent);

    static <T, U> ContextualTransformer<T, U> identity() {
        return (context, parent) -> parent;
    }

    static <T, U> ContextualTransformer<T, U> concat(ContextualTransformer<T, U> a, ContextualTransformer<T, U> b) {
        return (context, parent) -> a.transform(context, b.transform(context, parent));
    }

    static <T, U> ContextualTransformer<T, U> concat(ContextualTransformer<T, U>... transformers) {
        return Arrays.stream(transformers).reduce(identity(), ContextualTransformer::concat);
    }
}
