package io.izzel.aaa.api.visitor.impl;

import com.google.common.base.Preconditions;
import io.izzel.aaa.api.Attribute;
import io.izzel.aaa.api.AttributeTemplate;
import io.izzel.aaa.api.visitor.AttributeVisitor;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
public abstract class AbstractAttributeVisitor implements AttributeVisitor {
    protected final AttributeVisitor parent;

    protected AbstractAttributeVisitor(AttributeVisitor parent) {
        this.parent = Preconditions.checkNotNull(parent);
    }

    @Override
    public <T> void visit(Attribute<T> attribute, T data) {
        this.parent.visit(attribute, data);
    }

    @Override
    public AttributeVisitor.Templates visitTemplates() {
        return this.parent.visitTemplates();
    }

    @Override
    public void visitEnd() {
        this.parent.visitEnd();
    }

    @NonnullByDefault
    public abstract static class Templates implements AttributeVisitor.Templates {
        protected final AttributeVisitor.Templates parent;

        protected Templates(AttributeVisitor.Templates parent) {
            this.parent = Preconditions.checkNotNull(parent);
        }

        @Override
        public AttributeVisitor visitTemplate(AttributeTemplate template) {
            return this.parent.visitTemplate(template);
        }

        @Override
        public void visitTemplateEnd() {
            this.parent.visitTemplateEnd();
        }
    }
}
