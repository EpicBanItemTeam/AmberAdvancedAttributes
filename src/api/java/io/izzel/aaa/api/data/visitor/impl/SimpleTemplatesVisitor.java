package io.izzel.aaa.api.data.visitor.impl;

import com.google.common.base.Preconditions;
import io.izzel.aaa.api.Attribute;
import io.izzel.aaa.api.data.Template;
import io.izzel.aaa.api.data.visitor.MappingsVisitor;
import io.izzel.aaa.api.data.visitor.TemplatesVisitor;
import org.spongepowered.api.util.annotation.NonnullByDefault;

/**
 * An templates visitor which just appends a new attribute and corresponding data to a specific template.
 *
 * @param <T> the type of data
 */
@NonnullByDefault
public class SimpleTemplatesVisitor<T> extends AbstractTemplatesVisitor {
    private final Attribute<T> attribute;
    private final Iterable<? extends T> data;
    private final Template attributeTemplate;

    public SimpleTemplatesVisitor(TemplatesVisitor parent, Template template,
                                  Iterable<? extends T> dataCollection, Attribute<T> attribute) {
        super(parent);
        this.attribute = Preconditions.checkNotNull(attribute);
        this.data = Preconditions.checkNotNull(dataCollection);
        this.attributeTemplate = Preconditions.checkNotNull(template);
    }

    @Override
    public MappingsVisitor visitTemplate(Template template) {
        MappingsVisitor parent = super.visitTemplate(template);
        return !this.attributeTemplate.equals(template) ? parent : new AbstractMappingsVisitor(parent) {
            @Override
            public TemplatesVisitor visitTemplates() {
                for (T data : SimpleTemplatesVisitor.this.data) {
                    this.visitMapping(SimpleTemplatesVisitor.this.attribute, data);
                }
                return super.visitTemplates();
            }
        };
    }
}
