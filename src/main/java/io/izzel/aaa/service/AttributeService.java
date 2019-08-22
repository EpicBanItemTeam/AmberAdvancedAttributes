package io.izzel.aaa.service;

import com.google.inject.ImplementedBy;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Map;
import java.util.Optional;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
@ImplementedBy(AttributeServiceImpl.class)
public interface AttributeService {

    static AttributeService instance() {
        return Sponge.getServiceManager().provideUnchecked(AttributeService.class);
    }

    Map<String, Attribute<?>> getAttributes();

    @SuppressWarnings("unchecked")
    default <T extends DataSerializable> Optional<Attribute<T>> getAttributeById(String s) {
        return Optional.ofNullable((Attribute<T>) this.getAttributes().get(s));
    }
}
