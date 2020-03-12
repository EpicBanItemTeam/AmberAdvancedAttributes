package io.izzel.aaa.api.visitor;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import io.izzel.aaa.api.Attribute;
import io.izzel.aaa.api.AttributeTemplate;
import org.spongepowered.api.util.ResettableBuilder;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@NonnullByDefault
public class AttributeMap implements Consumer<AttributeVisitor> {
    private final ImmutableMap<AttributeTemplate, AttributeMap> templates;
    private final ImmutableListMultimap<Attribute<?>, Object> attributeData;

    private AttributeMap(Builder builder) {
        this.templates = ImmutableMap.copyOf(builder.templates);
        this.attributeData = ImmutableListMultimap.copyOf(builder.attributeData);
    }

    private <T> void visitEntry(AttributeVisitor visitor, Attribute<T> key, Object value) {
        visitor.visitAttributeData(key, key.getDataClass().cast(value));
    }

    @Override
    public void accept(AttributeVisitor visitor) {
        for (Map.Entry<Attribute<?>, Object> entry : this.attributeData.entries()) {
            this.visitEntry(visitor, entry.getKey(), entry.getValue());
        }
        for (Map.Entry<AttributeTemplate, AttributeMap> entry : this.templates.entrySet()) {
            entry.getValue().accept(visitor.visitTemplate(entry.getKey()));
        }
        visitor.visitEnd();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AttributeMap && ((AttributeMap) o).templates.equals(this.templates) && ((AttributeMap) o).attributeData.equals(this.attributeData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.templates, this.attributeData);
    }

    public static AttributeMap empty() {
        Builder builder = builder();
        builder.visitEnd();
        return builder.build();
    }

    public static Builder builder() {
        return new Builder(builder -> {
            // do nothing here
        });
    }

    @NonnullByDefault
    public static class Builder implements ResettableBuilder<AttributeMap, Builder>, AttributeVisitor {
        private boolean canVisitTemplate;
        private boolean canVisitAttributeData;
        private final Consumer<Builder> callback;
        private final Map<AttributeTemplate, AttributeMap> templates;
        private final ListMultimap<Attribute<?>, Object> attributeData;

        private Builder(Consumer<Builder> callback) {
            this.canVisitTemplate = true;
            this.canVisitAttributeData = true;
            this.templates = Maps.newLinkedHashMap();
            this.attributeData = LinkedListMultimap.create();
            this.callback = Preconditions.checkNotNull(callback);
        }

        public AttributeMap build() {
            Preconditions.checkState(!this.canVisitAttributeData, "visitEnd should be called");
            Preconditions.checkState(!this.canVisitTemplate, "visitEnd should be called");
            return new AttributeMap(this);
        }

        @Override
        public <T> void visitAttributeData(Attribute<T> attribute, T attributeData) {
            Preconditions.checkState(this.canVisitAttributeData, "cannot visit attribute data now");
            this.attributeData.put(attribute, attributeData);
        }

        @Override
        public AttributeVisitor visitTemplate(AttributeTemplate template) {
            this.canVisitAttributeData = false;
            Preconditions.checkState(this.canVisitTemplate, "cannot visit template now");
            return new Builder(builder -> this.templates.put(template, builder.build()));
        }

        @Override
        public void visitEnd() {
            this.canVisitAttributeData = this.canVisitTemplate = false;
            this.callback.accept(this);
        }

        @Override
        public Builder from(AttributeMap value) {
            this.reset();
            this.templates.putAll(value.templates);
            this.attributeData.putAll(value.attributeData);
            return this;
        }

        @Override
        public Builder reset() {
            this.templates.clear();
            this.attributeData.clear();
            this.canVisitTemplate = true;
            this.canVisitAttributeData = true;
            return this;
        }
    }
}
