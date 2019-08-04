package io.izzel.aaa.service;

import io.izzel.aaa.data.RangeValue;
import org.spongepowered.api.profile.GameProfile;

public class AttributeKeys {
    public static final Attribute<RangeValue> ATTACK;
    public static final Attribute<GameProfile> POSSESSION;

    private static RuntimeException error() {
        return new RuntimeException("The class is loaded too early! ");
    }

    static {
        AttributeService service = AttributeService.instance();

        ATTACK = service.<RangeValue>getAttributeById("aaa-attack").orElseThrow(AttributeKeys::error);
        POSSESSION = service.<GameProfile>getAttributeById("aaa-possession").orElseThrow(AttributeKeys::error);
    }
}
