package team.ebi.aaa.api.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import team.ebi.aaa.api.Attribute;
import team.ebi.aaa.api.data.visitor.MappingsVisitor;
import team.ebi.aaa.api.data.visitor.TemplatesVisitor;
import org.spongepowered.api.util.ResettableBuilder;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import team.ebi.aaa.api.Attribute;
import team.ebi.aaa.api.data.visitor.MappingsVisitor;
import team.ebi.aaa.api.data.visitor.TemplatesVisitor;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@NonnullByDefault
public final class Mappings implements Consumer<MappingsVisitor> {
    private final ImmutableMap<Template, Mappings> templates;
    private final ImmutableListMultimap<Attribute<?>, Object> attributeData;

    private Mappings(Builder builder) {
        this.templates = ImmutableMap.copyOf(builder.templates);
        this.attributeData = ImmutableListMultimap.copyOf(builder.attributeData);
    }

    private <T> void visitEntry(MappingsVisitor visitor, Attribute<T> key, Object value) {
        visitor.visitMapping(key, key.getDataClass().cast(value));
    }

    public static <T> Stream<T> dataStream(Mappings mappings, Attribute<T> attribute, boolean flatten) {
        Stream<T> current = mappings.attributeData.get(attribute).stream().map(attribute.getDataClass()::cast);
        if (flatten) {
            Stream<T> children = mappings.templates.values().stream().flatMap(m -> dataStream(m, attribute, true));
            return Stream.concat(current, children);
        }
        return current;
    }

    public Set<? extends Template> getTemplates() {
        return this.templates.keySet();
    }

    public Optional<Mappings> getAttributeMap(Template template) {
        return Optional.ofNullable(this.templates.get(template));
    }

    public <T> List<? extends T> getAttributeDataList(Attribute<T> attribute) {
        return dataStream(this, attribute, false).collect(ImmutableList.toImmutableList());
    }

    @Override
    public void accept(MappingsVisitor visitor) {
        for (Map.Entry<Attribute<?>, Object> entry : this.attributeData.entries()) {
            this.visitEntry(visitor, entry.getKey(), entry.getValue());
        }
        TemplatesVisitor templatesVisitor = visitor.visitTemplates();
        for (Map.Entry<Template, Mappings> entry : this.templates.entrySet()) {
            entry.getValue().accept(templatesVisitor.visitTemplate(entry.getKey()));
        }
        templatesVisitor.visitTemplateEnd();
        visitor.visitMappingsEnd();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Mappings && ((Mappings) o).templates.equals(this.templates) && ((Mappings) o).attributeData.equals(this.attributeData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.templates, this.attributeData);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Mappings{");
        for (Map.Entry<Attribute<?>, Object> entry : this.attributeData.entries()) {
            sb.append(entry.getKey().getDeserializationKey()).append("=");
            sb.append(entry.getValue()).append(", ");
        }
        sb.append("aaa-template=").append(this.templates);
        return sb.append("}").toString();
    }

    public static Mappings empty() {
        Builder builder = builder();
        {
            builder.visitTemplates().visitTemplateEnd();
            builder.visitMappingsEnd();
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder(builder -> {
            // do nothing here
        });
    }

    @NonnullByDefault
    public static final class Builder implements ResettableBuilder<Mappings, Builder>, MappingsVisitor {
        private final Consumer<Builder> callback;
        private final Map<Template, Mappings> templates;
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

        public Mappings build() {
            this.checkAndSwitch(State.VISIT_END, State.VISIT_END);
            return new Mappings(this);
        }

        @Override
        public <T> void visitMapping(Attribute<T> attribute, T data) {
            this.checkAndSwitch(State.VISIT, State.VISIT);
            this.attributeData.put(attribute, data);
        }

        @Override
        public TemplatesVisitor visitTemplates() {
            this.checkAndSwitch(State.VISIT, State.VISIT_TEMPLATE);
            return new TemplatesVisitor() {
                @Override
                public MappingsVisitor visitTemplate(Template template) {
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
        public void visitMappingsEnd() {
            this.checkAndSwitch(State.VISIT_TEMPLATE_END, State.VISIT_END);
            this.callback.accept(this);
        }

        @Override
        public Builder from(Mappings value) {
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
