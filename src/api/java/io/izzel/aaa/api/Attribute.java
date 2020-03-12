package io.izzel.aaa.api;

import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
public interface Attribute<T> {
    Class<T> getDataClass();
}
