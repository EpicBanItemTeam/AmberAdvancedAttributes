package io.izzel.aaa.byteitems;

import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.randombyte.byteitems.api.ByteItemsService;
import io.izzel.aaa.AmberAdvancedAttributes;
import io.izzel.amber.commons.i18n.AmberLocale;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.service.ServiceManager;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;

import java.io.Closeable;
import java.util.Optional;

/**
 * @author ustc_zzzz
 */
public final class ByteItemsProvider implements Provider<ByteItemsHandler> {
    private final CommandManager commandManager;
    private final ServiceManager serviceManager;
    private final PluginManager pluginManager;
    private final EventManager eventManager;
    private final Provider<AmberAdvancedAttributes> provider;
    private final AmberLocale locale;

    @Inject
    public ByteItemsProvider(AmberLocale locale, Provider<AmberAdvancedAttributes> provider, Game game) {
        this.commandManager = game.getCommandManager();
        this.serviceManager = game.getServiceManager();
        this.pluginManager = game.getPluginManager();
        this.eventManager = game.getEventManager();
        this.provider = provider;
        this.locale = locale;
    }

    @Override
    public ByteItemsHandler get() {
        return this.pluginManager.isLoaded("byte-items") ? new Present() : new Absent();
    }

    private CommandException error(String key, Object... args) {
        return new CommandException(this.locale.getAs(key, TypeToken.of(Text.class), args).get());
    }

    private final class Absent implements ByteItemsHandler {
        private final CommandException exception = error("commands.byte-items.unsupported");

        @Override
        public ItemStackSnapshot read(String id) {
            return ItemStackSnapshot.NONE;
        }

        @Override
        public ItemStackSnapshot save(String id, Player player) throws CommandException {
            throw this.exception;
        }
    }

    private final class Present implements ByteItemsHandler {
        private static final String PREFIX = "aaa-";

        @Override
        public ItemStackSnapshot read(String id) {
            ByteItemsService service = serviceManager.provideUnchecked(ByteItemsService.class);
            return service.get(PREFIX + id).orElse(ItemStackSnapshot.NONE);
        }

        @Override
        public ItemStackSnapshot save(String id, Player player) throws CommandException {
            Optional<ItemStack> optional = player.getItemInHand(HandTypes.MAIN_HAND);
            try (ByteItemsProvider.EventListener ignored = new ByteItemsProvider.EventListener(player)) {
                CommandMapping mapping = Iterables.getOnlyElement(commandManager.getAll("byte-items:bi"));
                ByteItemsService service = serviceManager.provideUnchecked(ByteItemsService.class);
                if (service.get(PREFIX + id).isPresent()) {
                    String arguments = "delete " + PREFIX + id;
                    mapping.getCallable().process(player, arguments);
                }
                if (optional.isPresent()) {
                    String arguments = "save " + PREFIX + id;
                    mapping.getCallable().process(player, arguments);
                    return optional.get().createSnapshot();
                }
                return ItemStackSnapshot.NONE;
            }
        }
    }

    public final class EventListener implements Closeable {
        private final Player player;

        private EventListener(Player player) {
            eventManager.registerListeners(provider.get(), this);
            this.player = player;
        }

        @Listener
        public void on(MessageChannelEvent event) {
            if (event.getOriginalChannel().getMembers().contains(this.player)) {
                event.setChannel(MessageChannel.TO_NONE);
            }
        }

        @Override
        public void close() {
            eventManager.unregisterListeners(this);
        }
    }
}
