package io.izzel.aaa.api.data;

import io.izzel.aaa.api.AttributeService;

public final class TemplateSlots {
    public static final TemplateSlot.Global GLOBAL;

    private static final TemplateSlot.Equipment MAIN_HAND;

    private static final TemplateSlot.Equipment OFF_HAND;

    private static final TemplateSlot.Equipment HEADWEAR;

    private static final TemplateSlot.Equipment CHESTPLATE;

    private static final TemplateSlot.Equipment LEGGINGS;

    private static final TemplateSlot.Equipment BOOTS;

    static {
        AttributeService service = AttributeService.instance();
        GLOBAL = (TemplateSlot.Global) service.getSlot("global").orElseThrow(IllegalStateException::new);
        MAIN_HAND = (TemplateSlot.Equipment) service.getSlot("main-hand").orElseThrow(IllegalStateException::new);
        OFF_HAND = (TemplateSlot.Equipment) service.getSlot("off-hand").orElseThrow(IllegalStateException::new);
        HEADWEAR = (TemplateSlot.Equipment) service.getSlot("headwear").orElseThrow(IllegalStateException::new);
        CHESTPLATE = (TemplateSlot.Equipment) service.getSlot("chestplate").orElseThrow(IllegalStateException::new);
        LEGGINGS = (TemplateSlot.Equipment) service.getSlot("leggings").orElseThrow(IllegalStateException::new);
        BOOTS = (TemplateSlot.Equipment) service.getSlot("boots").orElseThrow(IllegalStateException::new);
    }

    private TemplateSlots() {
        throw new IllegalStateException();
    }
}
