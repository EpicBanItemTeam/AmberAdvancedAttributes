package io.izzel.aaa.template;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.aaa.service.AttributeService;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
final class LoreTemplateServiceImpl implements LoreTemplateService {

    private final Map<String, BiFunction<Equipable, ItemStack, List<Text>>> map = new HashMap<>();

    @Inject
    public LoreTemplateServiceImpl(PluginContainer container, @ConfigDir(sharedRoot = false) Path dir) {
        Sponge.getServiceManager().setProvider(container, LoreTemplateService.class, this);
        var resolve = dir.resolve("templates.conf");
        try {
            if (!Files.exists(resolve)) {
                Files.createDirectories(dir);
                Files.createFile(resolve);
            }
            var root = HoconConfigurationLoader.builder().setPath(resolve).build().load();
            for (var entry : root.getChildrenMap().entrySet()) {
                try {
                    map.put(entry.getKey().toString(), compile(entry.getValue().getString().trim()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Text> eval(String name, Equipable entity, ItemStack itemStack) {
        return Optional.ofNullable(map.get(name)).map(it -> it.apply(entity, itemStack)).orElse(ImmutableList.of());
    }

    @Override
    public List<Text> eval(String name, Equipable entity, ItemStackSnapshot itemStack) {
        return Optional.ofNullable(map.get(name)).map(it -> it.apply(entity, itemStack.createStack())).orElse(ImmutableList.of());
    }

    private static final Pattern PATTERN = Pattern.compile("(?s)\\{\\{(?<c>(?!}}[^}]).)+}}(?<p>((?!\\{\\{).)+)?");
    private static final ScriptEngine JS = new ScriptEngineManager().getEngineByExtension("js");

    private BiFunction<Equipable, ItemStack, List<Text>> compile(String script) throws Exception {
        var matcher = PATTERN.matcher(script);
        var builder = new StringBuilder();
        while (matcher.find()) {
            if (builder.length() == 0) {
                appendEscaped(builder, script.substring(0, matcher.start()));
            }
            builder.append('{').append(matcher.group("c")).append('}');
            var p = matcher.group("p");
            if (p != null) appendEscaped(builder, p);
        }
        var compiled = ((Compilable) JS).compile(builder.toString());
        return (equipable, itemStack) -> {
            var sb = new StringBuilder();
            var service = AttributeService.instance();
            var bindings = compiled.getEngine().createBindings();
            bindings.put("entity", equipable);
            bindings.put("item", itemStack);
            bindings.put("attr", (Function<String, List<DataSerializable>>)
                    s -> service.getAttributeById(s).get().getValues(itemStack));
            //todo bindings.put("papi")
            bindings.put("print", (Consumer<String>) sb::append);
            bindings.put("println", (Consumer<String>) s -> sb.append(s).append('\n'));
            bindings.put("printf", (BiConsumer<String, Object>)
                    (s, o) -> sb.append(String.format(s, ((ScriptObjectMirror) o).values().toArray())));
            try {
                compiled.eval(bindings);
                return Arrays.stream(sb.toString().split("\\n"))
                        .map(TextSerializers.FORMATTING_CODE::deserialize).collect(Collectors.toList());
            } catch (ScriptException e) {
                e.printStackTrace();
                return ImmutableList.of();
            }
        };
    }

    private void appendEscaped(StringBuilder builder, String text) {
        builder.append("print(\"");
        for (var c : text.toCharArray()) {
            switch (c) {
                case '\r' -> {} // no crlf
                case '\'', '\"', '\\', '\n', '\t', '\b', '\f' -> builder.append('\\').append(c);
                default -> builder.append(c);
            }
        }
        builder.append("\");");
    }
}
