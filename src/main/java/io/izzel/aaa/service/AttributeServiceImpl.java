package io.izzel.aaa.service;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.aaa.collector.AttributeCollector;
import io.izzel.aaa.collector.AttributeCollectorImpl;
import io.izzel.aaa.data.*;
import io.izzel.aaa.template.LoreTemplateService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.game.state.GameAboutToStartServerEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.ServiceManager;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author ustc_zzzz
 */
@Singleton
@NonnullByDefault
public class AttributeServiceImpl implements AttributeService {

    private final Map<String, Attribute<?>> attributeMap = new LinkedHashMap<>();
    private final Map<String, Attribute<?>> attributeMapUnmodifiable = Collections.unmodifiableMap(this.attributeMap);

    @Inject
    public AttributeServiceImpl(PluginContainer container, ServiceManager serviceManager, EventManager eventManager, DataManager dataManager,
                                EquipmentSlotService slot, LoreTemplateService template) {
        Objects.requireNonNull(slot);
        Objects.requireNonNull(template);
        serviceManager.setProvider(container, AttributeService.class, this);
        eventManager.registerListener(container, GamePreInitializationEvent.class, event -> {
            Data.register(dataManager);
            RangeValue.register(dataManager);
            MarkerValue.register(dataManager);
            StringValue.register(dataManager);
            InlayData.register(dataManager);
        });
        eventManager.registerListener(container, GameAboutToStartServerEvent.class, event -> {
            try (var stackFrame = Sponge.getCauseStackManager().pushCauseFrame()) {
                Sponge.getEventManager().post(new RegistryEvent(stackFrame.getCurrentCause()));
            }
        });
    }

    @Override
    public Map<String, Attribute<?>> getAttributes() {
        return this.attributeMapUnmodifiable;
    }

    @Override
    public AttributeCollector createCollector(ItemStackSnapshot stackSnapshot) {
        return new AttributeCollectorImpl(stackSnapshot);
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
            var impl = new AttributeImpl<>(id, c, f);
            attributeMap.put(id, impl);
            return impl;
        }

        @Override
        public Cause getCause() {
            return cause;
        }
    }
}
