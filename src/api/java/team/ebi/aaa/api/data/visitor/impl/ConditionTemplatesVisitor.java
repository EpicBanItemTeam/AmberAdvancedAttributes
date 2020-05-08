package team.ebi.aaa.api.data.visitor.impl;

import com.google.common.base.Preconditions;
import team.ebi.aaa.api.data.Template;
import team.ebi.aaa.api.data.visitor.MappingsVisitor;
import team.ebi.aaa.api.data.visitor.TemplatesVisitor;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.function.BooleanSupplier;

/**
 * An templates visitor which just checks if a template should be kept or not.
 */
@NonnullByDefault
public class ConditionTemplatesVisitor extends AbstractTemplatesVisitor {
    private final BooleanSupplier cond;
    private final Template attributeTemplate;

    public ConditionTemplatesVisitor(TemplatesVisitor parent, Template template, BooleanSupplier condition) {
        super(parent);
        this.cond = Preconditions.checkNotNull(condition);
        this.attributeTemplate = Preconditions.checkNotNull(template);
    }

    @Override
    public MappingsVisitor visitTemplate(Template template) {
        boolean cond = !this.attributeTemplate.equals(template) || this.cond.getAsBoolean();
        return cond ? super.visitTemplate(template) : MappingsVisitor.EMPTY;
    }
}
