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

import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class TracingListener {

    private static final double P = Math.PI / 180D;
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
            Vector3d vec = player.getRotation(), pos = player.getPosition();
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

    private static Quaterniond euler(Vector3d rot) {
        double ex = rot.getX() * P / 2D;
        double ey = rot.getY() * P / 2D;
        double ez = rot.getZ() * P / 2D;
        double qx = sin(ex) * cos(ey) * cos(ez) + cos(ex) * sin(ey) * sin(ez);
        double qy = cos(ex) * sin(ey) * cos(ez) - sin(ex) * cos(ey) * sin(ez);
        double qz = cos(ex) * cos(ey) * sin(ez) - sin(ex) * sin(ey) * cos(ez);
        double qw = cos(ex) * cos(ey) * cos(ez) + sin(ex) * sin(ey) * sin(ez);
        return Quaterniond.from(qx, qy, qz, qw);
    }

    private static Vector3d rotate(Vector3d from, Vector3d to, double angle) {
        if (angle(from, to) <= 1) {
            return to;
        } else {
            Vector3d pivot = from.cross(to).normalize().mul(angle);
            return euler(pivot).rotate(from);
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
