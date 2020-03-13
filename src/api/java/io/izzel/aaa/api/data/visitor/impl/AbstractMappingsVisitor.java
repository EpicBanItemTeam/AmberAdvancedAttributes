package io.izzel.aaa.api.data.visitor.impl;

import com.google.common.base.Preconditions;
import io.izzel.aaa.api.Attribute;
import io.izzel.aaa.api.data.visitor.MappingsVisitor;
import io.izzel.aaa.api.data.visitor.TemplatesVisitor;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
public abstract class AbstractMappingsVisitor implements MappingsVisitor {
    protected final MappingsVisitor parent;

    protected AbstractMappingsVisitor(MappingsVisitor parent) {
        this.parent = Preconditions.checkNotNull(parent);
    }

    @Override
    public <T> void visitMapping(Attribute<T> attribute, T data) {
        this.parent.visitMapping(attribute, data);
    }

    @Override
    public TemplatesVisitor visitTemplates() {
        return this.parent.visitTemplates();
    }

    @Override
    public void visitMappingsEnd() {
        this.parent.visitMappingsEnd();
    }
}
