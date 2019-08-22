package io.izzel.aaa.service;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import io.izzel.aaa.data.Data;
import io.izzel.aaa.data.MarkerValue;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.data.StringValue;
import io.izzel.aaa.listener.ArrowListener;
import io.izzel.aaa.listener.AttackListener;
import io.izzel.aaa.listener.MiscListener;
import io.izzel.aaa.listener.PossessionListener;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.game.state.GameAboutToStartServerEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.PluginContainer;
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

    private final Map<String, Attribute<?>> attributeMap = new LinkedHashMap<>();
    private final Map<String, Attribute<?>> attributeMapUnmodifiable = Collections.unmodifiableMap(this.attributeMap);

    @Inject
    public AttributeServiceImpl(PluginContainer container, ServiceManager serviceManager, EventManager eventManager,
                                DataManager dataManager, Injector injector) {
        serviceManager.setProvider(container, AttributeService.class, this);
        eventManager.registerListener(container, GameInitializationEvent.class, event -> {
            Data.register(dataManager);
            RangeValue.register(dataManager);
            MarkerValue.register(dataManager);
            StringValue.register(dataManager);
        });
        eventManager.registerListener(container, GameAboutToStartServerEvent.class, event -> {
            try (CauseStackManager.StackFrame stackFrame = Sponge.getCauseStackManager().pushCauseFrame()) {
                Sponge.getEventManager().post(new RegistryEvent(stackFrame.getCurrentCause()));
            }
        });
        eventManager.registerListener(container, GameStartingServerEvent.class, event -> {
            eventManager.registerListeners(this, injector.getInstance(AttackListener.class));
            eventManager.registerListeners(this, injector.getInstance(ArrowListener.class));
            eventManager.registerListeners(this, injector.getInstance(PossessionListener.class));
            eventManager.registerListeners(this, injector.getInstance(MiscListener.class));
        });
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
