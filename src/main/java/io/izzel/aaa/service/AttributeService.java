package io.izzel.aaa.service;

import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Map;
import java.util.Optional;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public interface AttributeService {
    Map<String, Attribute<?>> getAttributes();

    @SuppressWarnings("unchecked")
    default <T extends DataSerializable> Optional<Attribute<T>> getAttributeById(String s) {
        return Optional.ofNullable((Attribute<T>) this.getAttributes().get(s));
    }
}
