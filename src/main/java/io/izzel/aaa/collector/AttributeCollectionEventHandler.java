package io.izzel.aaa.collector;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.util.annotation.NonnullByDefault;

/**
 * @author ustc_zzzz
 */
@Singleton
@NonnullByDefault
public class AttributeCollectionEventHandler {
    @Inject
    public AttributeCollectionEventHandler(PluginContainer container, EventManager manager) {
        manager.registerListener(container, AttributeCollectionEvent.class, this::onSuit);
        manager.registerListener(container, AttributeCollectionEvent.class, this::onTemplate);
    }

    private void onSuit(AttributeCollectionEvent event) {
        // TODO
    }

    private void onTemplate(AttributeCollectionEvent event) {
        // TODO
    }
}
