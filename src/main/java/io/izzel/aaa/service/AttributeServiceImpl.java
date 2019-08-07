package io.izzel.aaa.service;

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import io.izzel.aaa.Main;
import io.izzel.aaa.data.Data;
import io.izzel.aaa.data.ImmutableData;
import io.izzel.aaa.data.RangeValue;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class AttributeServiceImpl implements AttributeService {
    private final Main plugin;

    private final Map<String, Attribute<?>> attributeMap = new LinkedHashMap<>();
    private final Map<String, Attribute<?>> attributeMapUnmodifiable = Collections.unmodifiableMap(this.attributeMap);

    public AttributeServiceImpl(Main plugin) {
        this.plugin = plugin;
    }

    public void init() {
        Sponge.getServiceManager().setProvider(this.plugin, AttributeService.class, this);
        try (CauseStackManager.StackFrame stackFrame = Sponge.getCauseStackManager().pushCauseFrame()) {
            Sponge.getEventManager().post(new RegistryEvent(stackFrame.getCurrentCause()));
        }
        DataRegistration.builder()
                .dataClass(Data.class)
                .builder(new Data.Builder())
                .immutableClass(ImmutableData.class)
                .id("data").name("AmberAdvancedAttributes").build();
        Sponge.getDataManager().registerBuilder(RangeValue.class, RangeValue.builder());
    }

    @Override
    public Map<String, Attribute<?>> getAttributes() {
        return this.attributeMapUnmodifiable;
    }

    @NonnullByDefault
    private class RegistryEvent implements Attribute.RegistryEvent {
        private final Cause cause;

        private RegistryEvent(Cause currentCause) {
            cause = currentCause;
        }

        @Override
        public <T extends DataSerializable> Attribute<T> register(String id, TypeToken<T> t, AttributeToLoreFunction<T> f) {
            Preconditions.checkArgument(!attributeMap.containsKey(id), "Duplicate id");
            AttributeImpl<T> impl = new AttributeImpl<>(id, t, f);
            attributeMap.put(id, impl);
            return impl;
        }

        @Override
        public Cause getCause() {
            return cause;
        }
    }
}
