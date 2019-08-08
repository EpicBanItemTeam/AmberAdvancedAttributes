package io.izzel.aaa.listener;

import com.flowpowered.math.imaginary.Quaterniond;
import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Streams;
import io.izzel.aaa.Main;
import io.izzel.aaa.service.Attributes;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.projectile.LaunchProjectileEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.equipment.EquipmentInventory;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.scheduler.Task;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

public class TracingListener {

    private static final double DISTANCE = 64;

    @Listener
    public void on(LaunchProjectileEvent event, @Root Player player) {
        Inventory query = player.getInventory().query(QueryOperationTypes.INVENTORY_TYPE.of(EquipmentInventory.class));
        DoubleUnaryOperator operator = Streams.stream(query.<Slot>slots())
            .map(Inventory::peek)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Attributes.TRACING::getValues)
            .flatMap(Collection::stream)
            .map(it -> it.getFunction(ThreadLocalRandom.current()))
            .reduce(DoubleUnaryOperator::andThen)
            .orElse(DoubleUnaryOperator.identity());
        double tracing = operator.applyAsDouble(0D);
        if (tracing != 0D) {
            Vector3d rot = player.getHeadRotation();
            double pitch = rot.getX();
            double yaw = rot.getY();
            double xz = Math.cos(Math.toRadians(pitch));
            Vector3d vec = new Vector3d(-xz * Math.sin(Math.toRadians(yaw)), -Math.sin(Math.toRadians(pitch)), xz * Math.cos(Math.toRadians(yaw)));
            Vector3d pos = player.getPosition();
            Optional<Living> target = player.getNearbyEntities(DISTANCE).stream()
                .filter(Living.class::isInstance)
                .map(Living.class::cast)
                .min(Comparator.comparingDouble(it -> angle(vec, it.getLocation().getPosition().sub(pos))));
            if (target.isPresent()) {
                Projectile projectile = event.getTargetEntity();
                Task.builder().delayTicks(2).intervalTicks(4)
                    .execute(new RedirectProjectileTask(tracing, projectile, target.get())).submit(Main.INSTANCE);
            }
        }
    }

    private static double angle(Vector3d a, Vector3d b) {
        return Math.acos(a.dot(b) / (a.length() * b.length()));
    }

    private static Vector3d rotate(Vector3d from, Vector3d to, double angle) {
        if (angle(from, to) <= Math.toRadians(1)) {
            return to;
        } else {
            Vector3d pivot = from.cross(to).normalize().mul(angle);
            return Quaterniond.fromAngleRadAxis(Math.toRadians(angle), pivot).rotate(from);
        }
    }

    private static class RedirectProjectileTask implements Consumer<Task> {

        private final double tracingValue;
        private final WeakReference<Projectile> projectileWf;
        private final WeakReference<Living> targetWf;

        private RedirectProjectileTask(double tracingValue, Projectile projectile, Living target) {
            this.tracingValue = tracingValue;
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
            Vector3d direction = living.getLocation().getPosition().sub(velocity);
            projectile.setVelocity(rotate(velocity, direction, tracingValue));

        }

    }

}