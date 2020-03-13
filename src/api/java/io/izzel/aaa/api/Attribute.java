package io.izzel.aaa.api;

import com.google.common.reflect.TypeToken;
import io.izzel.aaa.api.context.ContextualTransformer;
import io.izzel.aaa.api.context.InitializationContext;
import io.izzel.aaa.api.context.SummaryContext;
import io.izzel.aaa.api.data.Template;
import io.izzel.aaa.api.data.TemplateSlot;
import io.izzel.aaa.api.data.visitor.impl.SimpleTemplatesVisitor;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;

@NonnullByDefault
public interface Attribute<T> {
    Class<T> getDataClass();

    String getDeserializationKey();

    boolean isCompatibleWith(TemplateSlot slot);

    default ContextualTransformer<InitializationContext> initAttributes(ConfigurationNode node) {
        return (context, parent) -> {
            try {
                Template template = context.getCurrentTemplate();
                T value = node.getValue(TypeToken.of(this.getDataClass()));
                return value == null ? parent : new SimpleTemplatesVisitor<>(parent, template, value, this);
            } catch (ObjectMappingException e) {
                return parent;
            }
        };
    }

    default ContextualTransformer<SummaryContext> summarizeAttributes() {
        return ContextualTransformer.identity(); // (context, parentVisitor) -> parentVisitor
    }

    @NonnullByDefault
    interface LoadEvent extends Event {
        default void register(Attribute<?> attribute) {
            this.getAttributesToBeRegistered().add(0, attribute);
        }

        List<Attribute<?>> getAttributesToBeRegistered();
    }
}
