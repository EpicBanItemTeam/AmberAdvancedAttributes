package io.izzel.aaa.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.game.state.GameAboutToStartServerEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.ServiceManager;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

@NonnullByDefault
@Singleton
final class EquipmentSlotServiceImpl implements EquipmentSlotService {

    private static final List<EquipmentType> EQUIPMENT_TYPES = ImmutableList.of(
            EquipmentTypes.HEADWEAR,
            EquipmentTypes.CHESTPLATE,
            EquipmentTypes.LEGGINGS,
            EquipmentTypes.BOOTS,
            EquipmentTypes.OFF_HAND,
            EquipmentTypes.MAIN_HAND
    );

    private final List<String> registered = new ArrayList<>();
    private final List<String> unmodifiable = Collections.unmodifiableList(registered);
    private final Map<String, Function<Equipable, Optional<ItemStack>>> getters = new HashMap<>();
    private final Map<String, BiFunction<Equipable, ItemStack, Boolean>> setters = new HashMap<>();

    @Inject
    public EquipmentSlotServiceImpl(PluginContainer container, ServiceManager serviceManager, EventManager eventManager) {
        serviceManager.setProvider(container, EquipmentSlotService.class, this);
        eventManager.registerListener(container, GameAboutToStartServerEvent.class, event -> {
            try (CauseStackManager.StackFrame stackFrame = Sponge.getCauseStackManager().pushCauseFrame()) {
                Sponge.getEventManager().post(new RegistryEventImpl(stackFrame.getCurrentCause()));
            }
        });
        eventManager.registerListener(container, EquipmentSlotService.RegistryEvent.class, event ->
                EQUIPMENT_TYPES.forEach(type -> event.register(type.getId(), equipable -> equipable.getEquipped(type),
                        (equipable, itemStack) -> equipable.equip(type, itemStack))));
    }

    @Override
    public List<String> slots() {
        return unmodifiable;
    }

    @Override
    public Optional<ItemStack> getItemStack(Equipable equipable, String slot) {
        return getters.get(slot).apply(equipable);
    }

    @Override
    public boolean setItemStack(Equipable equipable, String slot, ItemStack itemStack) {
        return setters.get(slot).apply(equipable, itemStack);
    }

    final class RegistryEventImpl implements RegistryEvent {

        private final Cause cause;

        RegistryEventImpl(Cause cause) {
            this.cause = cause;
        }

        @Override
        public Cause getCause() {
            return this.cause;
        }

        @Override
        public void register(String id, Function<Equipable, Optional<ItemStack>> getter, BiFunction<Equipable, ItemStack, Boolean> setter) {
            Preconditions.checkArgument(!registered.contains(id), "Duplicate slot");
            registered.add(id);
            getters.put(id, getter);
            setters.put(id, setter);
        }
    }

}
