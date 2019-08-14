package io.izzel.aaa.listener;

import com.google.inject.Singleton;
import io.izzel.aaa.Util;
import io.izzel.aaa.service.Attributes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;

import java.util.Random;

@Singleton
public class MiscListener {

    private final Random random = new Random();

    @Listener
    public void on(DamageEntityEvent event, @Getter("getTargetEntity") Entity to, @First EntityDamageSource source) {
        Entity from = source.getSource();
        if (from instanceof Equipable) {
            double burnRate = Util.allOf(((Equipable) from), Attributes.BURN_RATE);
            if (random.nextDouble() < burnRate) {
                double burn = Util.allOf(((Equipable) from), Attributes.BURN);
                to.offer(Keys.FIRE_TICKS, to.get(Keys.FIRE_TICKS).orElse(0) + (int) burn);
            }
        }
    }
}
