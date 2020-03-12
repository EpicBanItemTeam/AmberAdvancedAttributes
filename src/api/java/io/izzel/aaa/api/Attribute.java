package io.izzel.aaa.api;

import com.google.common.reflect.TypeToken;
import io.izzel.aaa.api.template.TemplateSlot;
import io.izzel.aaa.api.visitor.AttributeVisitor;
import io.izzel.aaa.api.visitor.impl.SimpleAttributeVisitor;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;
import java.util.function.UnaryOperator;

@NonnullByDefault
public interface Attribute<T> {
    Class<T> getDataClass();

    String getDeserializationKey();

    boolean isCompatibleWith(TemplateSlot slot);

    default UnaryOperator<AttributeVisitor> initAttributes(Player player, TemplateSlot slot, ConfigurationNode node) {
        return parent -> {
            try {
                T value = node.getValue(TypeToken.of(this.getDataClass()));
                return value == null ? parent : new SimpleAttributeVisitor<T>(parent, value, this);
            } catch (ObjectMappingException e) {
                return parent;
            }
        };
    }

    default UnaryOperator<AttributeVisitor.Templates> filterAttributeTemplates(Player player) {
        return UnaryOperator.identity(); // parent -> parent
    }

    @NonnullByDefault
    interface LoadEvent extends Event {
        default void register(Attribute<?> attribute) {
            this.getAttributesToBeRegistered().add(0, attribute);
        }

        List<Attribute<?>> getAttributesToBeRegistered();
    }
}
