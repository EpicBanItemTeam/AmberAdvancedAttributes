package io.izzel.aaa.byteitems;

import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import de.randombyte.byteitems.api.ByteItemsService;
import io.izzel.aaa.AmberAdvancedAttributes;
import io.izzel.aaa.itemdb.ItemDbService;
import io.izzel.amber.commons.i18n.AmberLocale;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.service.ServiceManager;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @author ustc_zzzz
 */
public final class ByteItemsProvider implements Provider<ByteItemsHandler> {
    private final CommandManager commandManager;
    private final ServiceManager serviceManager;
    private final PluginManager pluginManager;
    private final EventManager eventManager;
    private final PluginContainer container;
    private final AmberLocale locale;
    private final Injector injector;

    @Inject
    public ByteItemsProvider(AmberLocale locale, PluginContainer container, Game game, Injector injector) {
        this.commandManager = game.getCommandManager();
        this.serviceManager = game.getServiceManager();
        this.pluginManager = game.getPluginManager();
        this.eventManager = game.getEventManager();
        this.container = container;
        this.locale = locale;
        this.injector = injector;
    }

    @Override
    public ByteItemsHandler get() {
        ItemDbService service = injector.getInstance(ItemDbService.class);
        return new ByteItemsHandler() {
            @Override
            public ItemStackSnapshot read(String id) {
                return Optional.ofNullable(service.read(id)).map(ItemStack::createSnapshot).orElse(ItemStackSnapshot.NONE);
            }

            @Override
            public ItemStackSnapshot save(String id, Player player) {
                Optional<ItemStack> optional = player.getItemInHand(HandTypes.MAIN_HAND);
                if (optional.isPresent()) {
                    service.save(optional.get(), id);
                    return optional.get().createSnapshot();
                }
                return ItemStackSnapshot.NONE;
            }
        };
        // return this.pluginManager.isLoaded("byte-items") ? new Present() : new Absent();
    }

    private CommandException error(String key, Object... args) {
        return new CommandException(this.locale.getAs(key, TypeToken.of(Text.class), args).get());
    }

    public final class Absent implements ByteItemsHandler {
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

    public final class Present implements ByteItemsHandler {
        private static final String BYTE_ITEMS_PERMISSION = "byteitems";
        private static final String PREFIX = "aaa-";

        private final Context context;
        private final ThreadLocal<WeakReference<Player>> playerReference;

        private Present() {
            playerReference = ThreadLocal.withInitial(() -> new WeakReference<>(null));
            context = new Context(AmberAdvancedAttributes.ID, BYTE_ITEMS_PERMISSION);
            eventManager.registerListeners(container, this);
        }

        @Override
        public ItemStackSnapshot read(String id) {
            ByteItemsService service = serviceManager.provideUnchecked(ByteItemsService.class);
            return service.get(PREFIX + id).orElse(ItemStackSnapshot.NONE);
        }

        @Override
        public ItemStackSnapshot save(String id, Player player) throws CommandException {
            Optional<ItemStack> optional = player.getItemInHand(HandTypes.MAIN_HAND);
            playerReference.set(new WeakReference<>(player));
            try {
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
            } finally {
                playerReference.remove();
            }
        }

        @Listener
        public void on(MessageChannelEvent event) {
            Player player = playerReference.get().get();
            if (player != null && event.getOriginalChannel().getMembers().contains(player)) {
                event.setChannel(MessageChannel.TO_NONE);
            }
        }

        @Listener
        public void on(ClientConnectionEvent.Join event) {
            SubjectData subjectData = event.getTargetEntity().getTransientSubjectData();
            subjectData.setPermission(Collections.singleton(context), BYTE_ITEMS_PERMISSION, Tristate.TRUE);
        }

        @Listener
        public void on(GameStartingServerEvent event) {
            PermissionService permissionService = serviceManager.provideUnchecked(PermissionService.class);
            permissionService.registerContextCalculator(new ContextCalculator<Subject>() {
                @Override
                @NonnullByDefault
                public void accumulateContexts(Subject target, Set<Context> acc) {
                    Player player = playerReference.get().get();
                    if (player != null && player.getIdentifier().equals(target.getIdentifier())) {
                        acc.add(context);
                    }
                }

                @Override
                @NonnullByDefault
                public boolean matches(Context c, Subject target) {
                    if (Objects.equals(c, context)) {
                        Player player = playerReference.get().get();
                        return player != null && player.getIdentifier().equals(target.getIdentifier());
                    }
                    return false;
                }
            });
        }
    }
}
