package io.izzel.aaa.api.visitor.impl;

import com.google.common.base.Preconditions;
import io.izzel.aaa.api.Attribute;
import io.izzel.aaa.api.visitor.AttributeVisitor;
import org.spongepowered.api.util.annotation.NonnullByDefault;

/**
 * An attribute visitor which just appends a new attribute.
 *
 * @param <T> the type of data
 */
@NonnullByDefault
public class SimpleAttributeVisitor<T> extends AbstractAttributeVisitor {
    private final T data;
    private final Attribute<T> attribute;

    public SimpleAttributeVisitor(AttributeVisitor parent, T data, Attribute<T> attribute) {
        super(parent);
        this.data = Preconditions.checkNotNull(data);
        this.attribute = Preconditions.checkNotNull(attribute);
    }

    @Override
    public AttributeVisitor.Templates visitTemplates() {
        this.parent.visit(this.attribute, this.data);
        return super.visitTemplates();
    }
}
