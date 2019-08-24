package io.izzel.aaa.itemdb;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import ninja.leaping.configurate.ValueType;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Singleton
public final class ItemDbServiceProvider implements Provider<ItemDbService> {

    private final Object writeLock = new Object();
    private final PluginContainer container;
    private final Path path;
    private final Logger logger;

    private HoconConfigurationLoader loader;
    private Map<String, ItemStack> map = new LinkedHashMap<>();

    @Inject
    public ItemDbServiceProvider(PluginContainer container, @ConfigDir(sharedRoot = false) Path path,
                                 Game game, Logger logger) {
        this.container = container;
        this.path = path;
        this.logger = logger;
        game.getEventManager().registerListener(container, GameInitializationEvent.class, this::on);
        Task.builder().intervalTicks(20 * 3600).delayTicks(20 * 3600).execute(this::taskSave).submit(container);
        game.getEventManager().registerListener(container, GameStoppingServerEvent.class, event -> save(map));
    }

    private void on(GameInitializationEvent event) throws Exception {
        Path resolve = path.resolve("items.conf");
        loader = HoconConfigurationLoader.builder().setPath(resolve).build();
        CommentedConfigurationNode load = loader.load();
        for (Map.Entry<Object, ? extends CommentedConfigurationNode> entry : load.getChildrenMap().entrySet()) {
            if (entry.getValue().getValueType() != ValueType.NULL) {
                try {
                    DataContainer container = DataFormats.NBT.readFrom(Base64.getDecoder().wrap(
                            new ByteArrayInputStream(entry.getValue().getString().getBytes())));
                    Optional<ItemStack> deserialize = Sponge.getDataManager().deserialize(ItemStack.class, container);
                    map.put(entry.getKey().toString(), deserialize.get());
                } catch (Exception e) {
                    logger.error("Error deserializing item '{}': {}", entry.getKey().toString(), e);
                }
            }
        }
    }

    private void save(Map<String, ItemStack> map) {
        synchronized (writeLock) {
            Path cur = path.resolve("items.conf");
            try {
                if (Files.exists(cur)) {
                    Path old = path.resolve("items.conf.old");
                    Files.move(cur, old, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            CommentedConfigurationNode node = loader.createEmptyNode();
            for (Map.Entry<String, ItemStack> entry : map.entrySet()) {
                try {
                    ItemStack stack = entry.getValue();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    DataFormats.NBT.writeTo(Base64.getEncoder().wrap(out), stack.toContainer());
                    node.getNode(entry.getKey()).setValue(out.toString("utf-8"));
                } catch (Exception e) {
                    logger.error("Error serializing item '{}': {}", entry.getKey(), e);
                }
            }
            try {
                loader.save(node);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void taskSave() {
        LinkedHashMap<String, ItemStack> copy = new LinkedHashMap<>(map);
        Task.builder().async().execute(() -> save(copy)).submit(container);
    }

    @Override
    public ItemDbService get() {
        return new ItemDbService() {

            @Override
            public ItemStack read(String id) {
                return map.get(id);
            }

            @Override
            public void save(ItemStack stack, String id) {
                map.put(id, stack);
            }

            @Override
            public Map<String, ItemStack> list() {
                return Collections.unmodifiableMap(map);
            }

            @Override
            public void delete(String id) {
                map.remove(id);
            }
        };
    }

}
