package io.izzel.aaa.api.visitor;

import com.google.common.base.Preconditions;
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
    AttributeVisitor EMPTY = new AttributeVisitor() {
        @Override
        public <T> void visit(Attribute<T> attribute, T data) {
            Preconditions.checkNotNull(attribute);
            Preconditions.checkNotNull(data);
        }

        @Override
        public Templates visitTemplates() {
            return Templates.EMPTY;
        }

        @Override
        public void visitEnd() {
            // do nothing here
        }

        @Override
        public String toString() {
            return AttributeVisitor.class.getName() + ".EMPTY";
        }
    };

    <T> void visit(Attribute<T> attribute, T data);

    Templates visitTemplates();

    void visitEnd();

    interface Templates {
        Templates EMPTY = new Templates() {
            @Override
            public AttributeVisitor visitTemplate(AttributeTemplate template) {
                Preconditions.checkNotNull(template);
                return AttributeVisitor.EMPTY;
            }

            @Override
            public void visitTemplateEnd() {
                // do nothing here
            }

            @Override
            public String toString() {
                return Templates.class.getName() + ".EMPTY";
            }
        };

        AttributeVisitor visitTemplate(AttributeTemplate template);

        void visitTemplateEnd();
    }
}
