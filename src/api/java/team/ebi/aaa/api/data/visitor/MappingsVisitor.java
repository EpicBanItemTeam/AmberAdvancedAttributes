package team.ebi.aaa.api.data.visitor;

import com.google.common.base.Preconditions;
import team.ebi.aaa.api.Attribute;
import org.spongepowered.api.util.annotation.NonnullByDefault;

/**
 * Order:
 * - visitMapping
 * - visitTemplates
 *   - visitTemplate
 *   - visitTemplateEnd
 * - visitMappingsEnd
 */
@NonnullByDefault
public interface MappingsVisitor {
    MappingsVisitor EMPTY = new MappingsVisitor() {
        @Override
        public <T> void visitMapping(Attribute<T> attribute, T data) {
            Preconditions.checkNotNull(attribute);
            Preconditions.checkNotNull(data);
        }

        @Override
        public TemplatesVisitor visitTemplates() {
            return TemplatesVisitor.EMPTY;
        }

        @Override
        public void visitMappingsEnd() {
            // do nothing here
        }

        @Override
        public String toString() {
            return MappingsVisitor.class.getName() + ".EMPTY";
        }
    };

    <T> void visitMapping(Attribute<T> attribute, T data);

    TemplatesVisitor visitTemplates();

    void visitMappingsEnd();
}
