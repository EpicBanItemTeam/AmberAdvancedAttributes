package io.izzel.aaa.listener;

import com.flowpowered.math.GenericMath;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.aaa.collector.AttributeCollector;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.data.StringValue;
import io.izzel.aaa.service.Attributes;
import io.izzel.amber.commons.i18n.AmberLocale;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.AffectSlotEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.profile.GameProfile;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class PossessionListener {

    @Inject
    private AmberLocale locale;

    @Listener
    public void on(AffectSlotEvent event, @Root Player player) {
        event.getTransactions().stream().filter(Transaction::isValid).forEach(transaction -> {
            ItemStackSnapshot stack = transaction.getCustom().orElse(transaction.getDefault());
            List<GameProfile> possession = new ArrayList<>();
            List<StringValue> permsCap = new ArrayList<>();
            List<RangeValue> levelCap = new ArrayList<>();
            AttributeCollector.of(stack)
                    .collect(Attributes.POSSESSION, possession)
                    .collect(Attributes.PERMISSION_CAP, permsCap)
                    .collect(Attributes.LEVEL_CAP, levelCap)
                    .submit();
            if (possession.stream().anyMatch(it -> !it.getUniqueId().equals(player.getUniqueId()))) {
                transaction.setValid(false);
                locale.to(player, "attributes.possession.no-permission");
            }
            if (!permsCap.stream().map(StringValue::getString).allMatch(player::hasPermission)) {
                transaction.setValid(false);
                locale.to(player, "attributes.permission-cap.no-perm");
            }
            int level = player.get(Keys.EXPERIENCE_LEVEL).orElse(0);
            if (!levelCap.stream().allMatch(v -> {
                if (v.getSize() > GenericMath.DBL_EPSILON) {
                    return level >= v.getLowerBound() && level <= v.getUpperBound();
                } else {
                    return level >= v.getLowerBound();
                }
            })) {
                transaction.setValid(false);
                locale.to(player, "attributes.level-cap.no-perm");
            }
        });
    }


}
