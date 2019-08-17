package io.izzel.aaa;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.izzel.aaa.command.AttributeCommands;
import io.izzel.aaa.listener.ArrowListener;
import io.izzel.aaa.listener.AttackListener;
import io.izzel.aaa.listener.MiscListener;
import io.izzel.aaa.listener.PossessionListener;
import io.izzel.aaa.service.AttributeServiceImpl;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "amberadvancedattributes", description = "An AmberAdvancedAttributes item attribute plugin.")
public class Main {
    private final AttributeServiceImpl service;
    private final AttributeCommands commands;
    private final EventManager eventManager;
    private final Injector injector;


    @Inject
    public Main(AttributeServiceImpl service, AttributeCommands commands, EventManager e, Injector i) {
        this.commands = commands;
        this.service = service;
        this.eventManager = e;
        this.injector = i;
    }

    @Listener
    public void on(GameInitializationEvent event) {
        this.commands.init();
        this.service.init();
    }

    @Listener
    public void on(GameStartingServerEvent event) {
        this.eventManager.registerListeners(this, this.injector.getInstance(AttackListener.class));
        this.eventManager.registerListeners(this, this.injector.getInstance(ArrowListener.class));
        this.eventManager.registerListeners(this, this.injector.getInstance(PossessionListener.class));
        this.eventManager.registerListeners(this, this.injector.getInstance(MiscListener.class));
    }
}
