package io.izzel.aaa.listener;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import io.izzel.aaa.Util;
import io.izzel.aaa.service.Attributes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierTypes;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.cause.First;

import java.util.Random;

@Singleton
public class AttackListener {

    @Listener
    public void on(DamageEntityEvent event, @First Player player) {
        Util.items(player).forEach(itemStack -> {
            Attributes.ATTACK.getValues(itemStack).forEach(v ->
                event.addDamageModifierBefore(DamageModifier.builder().cause(event.getCause())
                        .type(DamageModifierTypes.WEAPON_ENCHANTMENT).item(itemStack).build(),
                    v.getFunction(this.random), ImmutableSet.of()));
        });
    }



    private final Random random = new Random();
}
