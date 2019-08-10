package io.izzel.aaa.listener;

import com.flowpowered.math.imaginary.Quaterniond;
import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Streams;
import com.google.inject.Singleton;
import io.izzel.aaa.Main;
import io.izzel.aaa.service.Attributes;
import org.spongepowered.api.data.property.AbstractProperty;
import org.spongepowered.api.data.property.entity.EyeLocationProperty;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentInventory;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.scheduler.Task;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class TracingListener {

    private static final double DISTANCE = 64;

    @Listener
    public void on(SpawnEntityEvent event) {
        List<Projectile> projectiles = event.getEntities().stream().filter(Projectile.class::isInstance).map(Projectile.class::cast).collect(Collectors.toList());
        if (!projectiles.isEmpty()) {
            event.getContext().get(EventContextKeys.OWNER).flatMap(User::getPlayer).ifPresent(player -> {
                double[] tracing = {0D};
                Stream.concat(Streams.stream(player.getInventory().query(QueryOperationTypes.INVENTORY_TYPE.of(EquipmentInventory.class)).slots())
                    .map(Inventory::peek), Stream.of(player.getItemInHand(HandTypes.MAIN_HAND), player.getItemInHand(HandTypes.OFF_HAND)))
                    .filter(Optional::isPresent).map(Optional::get)
                    .map(Attributes.TRACING::getValues)
                    .flatMap(Collection::stream)
                    .map(it -> it.getFunction(ThreadLocalRandom.current()))
                    .forEach(it -> tracing[0] += it.applyAsDouble(tracing[0]));
                if (tracing[0] != 0D) {
                    Vector3d rot = player.getHeadRotation();
                    double pitch = rot.getX();
                    double yaw = rot.getY();
                    double xz = Math.cos(Math.toRadians(pitch));
                    Vector3d vec = new Vector3d(-xz * Math.sin(Math.toRadians(yaw)), -Math.sin(Math.toRadians(pitch)), xz * Math.cos(Math.toRadians(yaw)));
                    Vector3d pos = player.getPosition();
                    Optional<Living> target = player.getNearbyEntities(DISTANCE).stream()
                        .filter(it -> it != player)
                        .filter(Living.class::isInstance)
                        .map(Living.class::cast)
                        .min(Comparator.comparingDouble(it -> angle(vec, it.getLocation().getPosition().sub(pos))));
                    if (target.isPresent()) {
                        for (Projectile projectile : projectiles) {
                            Task.builder().delayTicks(1).intervalTicks(1)
                                .execute(new RedirectProjectileTask(tracing[0], projectile, target.get())).submit(Main.INSTANCE);
                        }
                    }
                }
            });
        }
    }

    private static double angle(Vector3d a, Vector3d b) {
        return Math.acos(a.dot(b) / (a.length() * b.length()));
    }

    private static Vector3d rotate(Vector3d from, Vector3d to, double angle) {
        if (angle(from, to) <= Math.toRadians(1)) {
            return to;
        } else {
            return Quaterniond.fromAngleRadAxis(Math.toRadians(angle), from.cross(to)).rotate(from);
        }
    }

    private static class RedirectProjectileTask implements Consumer<Task> {

        private final double tracingValue;
        private final WeakReference<Projectile> projectileWf;
        private final WeakReference<Living> targetWf;

        private RedirectProjectileTask(double tracingValue, Projectile projectile, Living target) {
            this.tracingValue = tracingValue / 10D;
            this.projectileWf = new WeakReference<>(projectile);
            this.targetWf = new WeakReference<>(target);
        }

        @Override
        public void accept(Task task) {
            Projectile projectile = projectileWf.get();
            Living living = targetWf.get();
            if (projectile == null || living == null) {
                task.cancel();
                return;
            }

            Vector3d velocity = projectile.getVelocity();
            Vector3d direction = living.getLocation().getPosition().sub(
                projectile.getProperty(EyeLocationProperty.class)
                    .map(AbstractProperty::getValue)
                    .orElse(projectile.getLocation().getPosition())
            );
            projectile.setVelocity(rotate(velocity, direction, tracingValue));

        }

    }

}
