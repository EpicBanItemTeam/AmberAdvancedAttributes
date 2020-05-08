package team.ebi.aaa.api;

import com.google.common.reflect.TypeToken;
import team.ebi.aaa.api.context.ContextualTransformer;
import team.ebi.aaa.api.context.InitializationContext;
import team.ebi.aaa.api.context.SummaryContext;
import team.ebi.aaa.api.data.Template;
import team.ebi.aaa.api.data.TemplateSlot;
import team.ebi.aaa.api.data.visitor.TemplatesVisitor;
import team.ebi.aaa.api.data.visitor.impl.SimpleTemplatesVisitor;
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

    default ContextualTransformer<InitializationContext, TemplatesVisitor> initAttributes(ConfigurationNode node) throws ObjectMappingException {
        List<T> value = node.getList(TypeToken.of(this.getDataClass()));
        if (value.contains(null)) throw new ObjectMappingException("Null object at index " + value.indexOf(null));
        return value.isEmpty() ? ContextualTransformer.identity() : (context, parent) -> {
            Template template = context.getCurrentTemplate();
            return new SimpleTemplatesVisitor<>(parent, template, value, this);
        };
    }

    default ContextualTransformer<SummaryContext, TemplatesVisitor> summarizeAttributes() {
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
