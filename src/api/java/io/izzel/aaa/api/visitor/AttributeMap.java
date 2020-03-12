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
public final class AttributeMap implements Consumer<AttributeVisitor> {
    private final ImmutableMap<AttributeTemplate, AttributeMap> templates;
    private final ImmutableListMultimap<Attribute<?>, Object> attributeData;

    private AttributeMap(Builder builder) {
        this.templates = ImmutableMap.copyOf(builder.templates);
        this.attributeData = ImmutableListMultimap.copyOf(builder.attributeData);
    }

    private <T> void visitEntry(AttributeVisitor visitor, Attribute<T> key, Object value) {
        visitor.visit(key, key.getDataClass().cast(value));
    }

    @Override
    public void accept(AttributeVisitor visitor) {
        for (Map.Entry<Attribute<?>, Object> entry : this.attributeData.entries()) {
            this.visitEntry(visitor, entry.getKey(), entry.getValue());
        }
        AttributeVisitor.Templates templatesVisitor = visitor.visitTemplates();
        for (Map.Entry<AttributeTemplate, AttributeMap> entry : this.templates.entrySet()) {
            entry.getValue().accept(templatesVisitor.visitTemplate(entry.getKey()));
        }
        templatesVisitor.visitTemplateEnd();
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
        {
            builder.visitTemplates().visitTemplateEnd();
            builder.visitEnd();
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder(builder -> {
            // do nothing here
        });
    }

    @NonnullByDefault
    public static final class Builder implements ResettableBuilder<AttributeMap, Builder>, AttributeVisitor {
        private final Consumer<Builder> callback;
        private final Map<AttributeTemplate, AttributeMap> templates;
        private final ListMultimap<Attribute<?>, Object> attributeData;

        private State state = State.VISIT;

        private Builder(Consumer<Builder> callback) {
            this.templates = Maps.newLinkedHashMap();
            this.attributeData = LinkedListMultimap.create();
            this.callback = Preconditions.checkNotNull(callback);
        }

        private void checkAndSwitch(State checkState, State switchState) {
            Preconditions.checkState(this.state == checkState, this.state.message);
            this.state = switchState;
        }

        public AttributeMap build() {
            this.checkAndSwitch(State.VISIT_END, State.VISIT_END);
            return new AttributeMap(this);
        }

        @Override
        public <T> void visit(Attribute<T> attribute, T data) {
            this.checkAndSwitch(State.VISIT, State.VISIT);
            this.attributeData.put(attribute, data);
        }

        @Override
        public AttributeVisitor.Templates visitTemplates() {
            this.checkAndSwitch(State.VISIT, State.VISIT_TEMPLATE);
            return new AttributeVisitor.Templates() {
                @Override
                public AttributeVisitor visitTemplate(AttributeTemplate template) {
                    Builder.this.checkAndSwitch(State.VISIT_TEMPLATE, State.VISIT_TEMPLATE);
                    return new Builder(builder -> Builder.this.templates.put(template, builder.build()));
                }

                @Override
                public void visitTemplateEnd() {
                    Builder.this.checkAndSwitch(State.VISIT_TEMPLATE, State.VISIT_TEMPLATE_END);
                }
            };
        }

        @Override
        public void visitEnd() {
            this.checkAndSwitch(State.VISIT_TEMPLATE_END, State.VISIT_END);
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
            this.state = State.VISIT;
            return this;
        }

        private enum State {
            VISIT("only visitAttributeData and visitTemplates allowed here"),
            VISIT_TEMPLATE("only visitTemplate and visitTemplateEnd allowed here"),
            VISIT_TEMPLATE_END("only visitEnd allowed here"),
            VISIT_END("only build method allowed here");

            private final String message;

            State(String message) {
                this.message = message;
            }
        }
    }
}
