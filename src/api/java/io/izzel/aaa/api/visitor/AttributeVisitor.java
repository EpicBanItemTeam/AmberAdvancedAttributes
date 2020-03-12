package io.izzel.aaa.api.visitor;

import io.izzel.aaa.api.Attribute;
import io.izzel.aaa.api.AttributeTemplate;

/**
 * Order: visitAttributeData, visitTemplate, visitEnd
 */
public interface AttributeVisitor {
    <T> void visitAttributeData(Attribute<T> attribute, T attributeData);

    AttributeVisitor visitTemplate(AttributeTemplate template);

    void visitEnd();
}
