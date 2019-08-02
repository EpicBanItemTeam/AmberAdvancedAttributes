package io.izzel.aaa;

import com.google.inject.Inject;
import io.izzel.aaa.service.AttributeServiceImpl;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.plugin.Plugin;

import java.util.logging.Logger;

@Plugin(id = "amberadvancedattributes")
public class Main {
    public final Logger logger;
    public final AttributeServiceImpl service;

    @Inject
    public Main(Logger logger) {
        this.logger = logger;
        this.service = new AttributeServiceImpl(this);
    }

    @Listener
    public void on(GamePostInitializationEvent event) {
        this.service.init();
    }
}
