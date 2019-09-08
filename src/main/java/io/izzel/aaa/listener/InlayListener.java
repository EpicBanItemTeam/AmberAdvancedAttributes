package io.izzel.aaa.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.izzel.aaa.collector.AttributeCollector;
import io.izzel.aaa.data.InlayData;
import io.izzel.aaa.data.MarkerValue;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.data.StringValue;
import io.izzel.aaa.service.Attributes;
import io.izzel.amber.commons.i18n.AmberLocale;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Singleton
public class InlayListener {

    private final Random random = new Random();
    @Inject
    private AmberLocale locale;

    @Listener
    public void on(ChangeInventoryEvent.SwapHand event, @First Player player) {
        if (event.getTransactions().size() == 2) {
            SlotTransaction offhand = event.getTransactions().get(0);
            SlotTransaction mainhand = event.getTransactions().get(1);
            List<MarkerValue> gem = new ArrayList<>();
            List<RangeValue.Fixed> success = new ArrayList<>();
            List<InlayData> targets = new ArrayList<>();
            List<StringValue> id = new ArrayList<>();
            AttributeCollector.of(offhand.getOriginal())
                    .collect(Attributes.INLAY_GEM, gem)
                    .collect(Attributes.INLAY_SUCCESS, success)
                    .collect(Attributes.INLAY, targets)
                    .collect(Attributes.ID, id)
                    .submit();
            if (!gem.isEmpty()) {
                if (id.isEmpty()) {
                    locale.to(player, "attributes.inlay.not-saved", offhand.getOriginal());
                    return;
                }
                ItemStack original = mainhand.getOriginal().createStack();
                List<InlayData> slots = new ArrayList<>();
                List<MarkerValue> mainGem = new ArrayList<>();
                AttributeCollector.of(original).collect(Attributes.INLAY, slots)
                        .collect(Attributes.INLAY_GEM, mainGem).submit();
                if (slots.isEmpty() || !mainGem.isEmpty()) return;
                List<String> targetNames = targets.stream().map(InlayData::getSlot).collect(Collectors.toList());
                Optional<InlayData> first = slots.stream().filter(it -> !it.getGem().isPresent())
                        .filter(it -> targetNames.contains(it.getSlot()))
                        .findFirst();
                if (first.isPresent()) {
                    double suc = success.stream().mapToDouble(RangeValue::getLowerBound).max().orElse(1D);
                    if (random.nextDouble() < suc) {
                        InlayData data = first.get();
                        List<InlayData> newAttr = slots.stream()
                                .filter(it -> !it.getSlot().equals(data.getSlot()))
                                .collect(Collectors.toList());
                        newAttr.add(InlayData.of(data.getSlot(), id.get(0).getString()));
                        Attributes.INLAY.setValues(original, newAttr);
                        mainhand.setCustom(original);
                        offhand.setCustom(ItemStackSnapshot.NONE);
                        locale.to(player, "attributes.inlay.success", offhand.getOriginal(), mainhand.getOriginal());
                    } else {
                        offhand.setCustom(ItemStackSnapshot.NONE);
                        locale.to(player, "attributes.inlay.fail", offhand.getOriginal());
                    }
                }
            }
        }
    }

}
