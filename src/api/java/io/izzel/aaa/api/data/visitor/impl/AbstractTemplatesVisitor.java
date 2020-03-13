package io.izzel.aaa.api.data.visitor.impl;

import com.google.common.base.Preconditions;
import io.izzel.aaa.api.data.Template;
import io.izzel.aaa.api.data.visitor.MappingsVisitor;
import io.izzel.aaa.api.data.visitor.TemplatesVisitor;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
public abstract class AbstractTemplatesVisitor implements TemplatesVisitor {
    protected final TemplatesVisitor parent;

    protected AbstractTemplatesVisitor(TemplatesVisitor parent) {
        this.parent = Preconditions.checkNotNull(parent);
    }

    @Override
    public MappingsVisitor visitTemplate(Template template) {
        return this.parent.visitTemplate(template);
    }

    @Override
    public void visitTemplateEnd() {
        this.parent.visitTemplateEnd();
    }
}
