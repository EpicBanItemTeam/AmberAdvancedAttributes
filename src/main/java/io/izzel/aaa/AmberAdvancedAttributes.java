package io.izzel.aaa;

import com.google.inject.Inject;
import io.izzel.aaa.command.AttributeCommands;
import io.izzel.aaa.service.AttributeService;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "amberadvancedattributes", description = "An AmberAdvancedAttributes item attribute plugin.")
public class AmberAdvancedAttributes {

    @Inject
    public AmberAdvancedAttributes(AttributeService service, AttributeCommands commands) {
    }
}
