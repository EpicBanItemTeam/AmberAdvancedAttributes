package io.izzel.aaa;

import com.google.inject.Inject;
import io.izzel.aaa.collector.AttributeCollectionEventHandler;
import io.izzel.aaa.command.AttributeCommands;
import io.izzel.aaa.listener.AttributeListeners;
import io.izzel.aaa.service.AttributeService;
import io.izzel.aaa.util.internal.org.bstats.sponge.Metrics;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;

import java.util.Objects;

@Plugin(id = AmberAdvancedAttributes.ID,
        name = AmberAdvancedAttributes.NAME,
        description = AmberAdvancedAttributes.DESC,
        dependencies = @Dependency(id = "byte-items", optional = true))
public class AmberAdvancedAttributes {
    public static final String ID = "amberadvancedattributes";
    public static final String NAME = "AmberAdvancedAttributes";
    public static final String DESC = "An advanced attribute plugin for items.";

    @Inject
    public AmberAdvancedAttributes(AttributeListeners listeners,
                                   AttributeCommands commands,
                                   AttributeService service,
                                   AttributeCollectionEventHandler handler,
                                   Metrics metrics) {
        Objects.requireNonNull(listeners);
        Objects.requireNonNull(commands);
        Objects.requireNonNull(service);
        Objects.requireNonNull(handler);
        Objects.requireNonNull(metrics);
    }
}
