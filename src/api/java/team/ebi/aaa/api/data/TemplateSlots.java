package team.ebi.aaa.api.data;

import team.ebi.aaa.api.AttributeService;

public final class TemplateSlots {
    public static final TemplateSlot.Global GLOBAL;

    public static final TemplateSlot.Equipment MAIN_HAND;

    public static final TemplateSlot.Equipment OFF_HAND;

    public static final TemplateSlot.Equipment HEAD;

    public static final TemplateSlot.Equipment CHESTPLATE;

    public static final TemplateSlot.Equipment LEGGINGS;

    public static final TemplateSlot.Equipment BOOTS;

    static {
        AttributeService service = AttributeService.instance();
        GLOBAL = (TemplateSlot.Global) service.getSlot("global").orElseThrow(IllegalStateException::new);
        MAIN_HAND = (TemplateSlot.Equipment) service.getSlot("main-hand").orElseThrow(IllegalStateException::new);
        OFF_HAND = (TemplateSlot.Equipment) service.getSlot("off-hand").orElseThrow(IllegalStateException::new);
        HEAD = (TemplateSlot.Equipment) service.getSlot("head").orElseThrow(IllegalStateException::new);
        CHESTPLATE = (TemplateSlot.Equipment) service.getSlot("chestplate").orElseThrow(IllegalStateException::new);
        LEGGINGS = (TemplateSlot.Equipment) service.getSlot("leggings").orElseThrow(IllegalStateException::new);
        BOOTS = (TemplateSlot.Equipment) service.getSlot("boots").orElseThrow(IllegalStateException::new);
    }

    private TemplateSlots() {
        throw new IllegalStateException();
    }
}
