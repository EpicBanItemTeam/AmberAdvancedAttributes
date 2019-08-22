package io.izzel.aaa;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.randombyte.byteitems.api.ByteItemsService;
import io.izzel.aaa.command.AttributeCommands;
import io.izzel.aaa.listener.*;
import io.izzel.aaa.service.AttributeService;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
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
    public AmberAdvancedAttributes(AttributeListeners listeners, AttributeCommands commands, AttributeService service) {
        Objects.requireNonNull(listeners);
        Objects.requireNonNull(commands);
        Objects.requireNonNull(service);
    }
}
