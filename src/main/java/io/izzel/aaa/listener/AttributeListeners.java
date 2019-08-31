package io.izzel.aaa.listener;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.plugin.PluginContainer;

/**
 * @author ustc_zzzz
 */
@Singleton
public class AttributeListeners {
    @Inject
    public AttributeListeners(PluginContainer container, EventManager eventManager, Injector injector) {
        eventManager.registerListeners(container, injector.getInstance(AttackListener.class));
        eventManager.registerListeners(container, injector.getInstance(ArrowListener.class));
        eventManager.registerListeners(container, injector.getInstance(PossessionListener.class));
        eventManager.registerListeners(container, injector.getInstance(MiscListener.class));
        eventManager.registerListeners(container, injector.getInstance(InlayListener.class));
    }
}
