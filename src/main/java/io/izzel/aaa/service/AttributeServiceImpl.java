package io.izzel.aaa.service;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.izzel.aaa.Main;
import io.izzel.aaa.data.Data;
import io.izzel.aaa.data.ImmutableData;
import io.izzel.aaa.data.MarkerValue;
import io.izzel.aaa.data.RangeValue;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.game.state.GameAboutToStartServerEvent;
import org.spongepowered.api.service.ServiceManager;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author ustc_zzzz
 */
@Singleton
@NonnullByDefault
public class AttributeServiceImpl implements AttributeService {
    private final Provider<Main> pluginProvider;
    private final ServiceManager serviceManager;
    private final EventManager eventManager;
    private final DataManager dataManager;

    private final Map<String, Attribute<?>> attributeMap = new LinkedHashMap<>();
    private final Map<String, Attribute<?>> attributeMapUnmodifiable = Collections.unmodifiableMap(this.attributeMap);

    @Inject
    public AttributeServiceImpl(Provider<Main> pluginProvider, ServiceManager s, EventManager e, DataManager d) {
        this.pluginProvider = pluginProvider;
        this.serviceManager = s;
        this.eventManager = e;
        this.dataManager = d;
    }

    public void init() {
        this.registerData();
        this.serviceManager.setProvider(this.pluginProvider.get(), AttributeService.class, this);
        this.eventManager.registerListener(this.pluginProvider.get(), GameAboutToStartServerEvent.class, event -> {
            try (CauseStackManager.StackFrame stackFrame = Sponge.getCauseStackManager().pushCauseFrame()) {
                Sponge.getEventManager().post(new RegistryEvent(stackFrame.getCurrentCause()));
            }
        });
    }

    private void registerData() {
        DataRegistration.builder()
                .dataClass(Data.class)
                .builder(new Data.Builder())
                .immutableClass(ImmutableData.class)
                .id("data").name("AmberAdvancedAttributes").build();
        this.dataManager.registerBuilder(RangeValue.class, RangeValue.builder());
        this.dataManager.registerBuilder(MarkerValue.class, MarkerValue.builder());
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
        public <T extends DataSerializable> Attribute<T> register(String id, Class<T> c, AttributeToLoreFunction<T> f) {
            Preconditions.checkArgument(!attributeMap.containsKey(id), "Duplicate id");
            AttributeImpl<T> impl = new AttributeImpl<>(id, c, f);
            attributeMap.put(id, impl);
            return impl;
        }

        @Override
        public Cause getCause() {
            return cause;
        }
    }
}
