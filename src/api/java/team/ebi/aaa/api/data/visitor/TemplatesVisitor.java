package team.ebi.aaa.api.data.visitor;

import com.google.common.base.Preconditions;
import team.ebi.aaa.api.data.Template;
import org.spongepowered.api.util.annotation.NonnullByDefault;

/**
 * Order:
 * - visitTemplate
 *   - visitMapping
 *   - visitTemplates
 *   - visitMappingsEnd
 * - visitTemplateEnd
 */
@NonnullByDefault
public interface TemplatesVisitor {
    TemplatesVisitor EMPTY = new TemplatesVisitor() {
        @Override
        public MappingsVisitor visitTemplate(Template template) {
            Preconditions.checkNotNull(template);
            return MappingsVisitor.EMPTY;
        }

        @Override
        public void visitTemplateEnd() {
            // do nothing here
        }

        @Override
        public String toString() {
            return TemplatesVisitor.class.getName() + ".EMPTY";
        }
    };

    MappingsVisitor visitTemplate(Template template);

    void visitTemplateEnd();
}
