package io.izzel.aaa.api.visitor;

import io.izzel.aaa.api.Attribute;
import io.izzel.aaa.api.AttributeTemplate;

/**
 * Order:
 * - visit
 * - visitTemplates
 *   - visitTemplate
 *   - visitTemplateEnd
 * - visitEnd
 */
public interface AttributeVisitor {
    <T> void visit(Attribute<T> attribute, T data);

    Templates visitTemplates();

    void visitEnd();

    interface Templates {
        AttributeVisitor visitTemplate(AttributeTemplate template);

        void visitTemplateEnd();
    }
}
