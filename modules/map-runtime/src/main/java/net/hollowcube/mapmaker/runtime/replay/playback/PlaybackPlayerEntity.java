package net.hollowcube.mapmaker.runtime.replay.playback;

import dev.hollowcube.replay.event.SetItemEvent;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;

import java.util.Arrays;
import java.util.UUID;

public class PlaybackPlayerEntity extends LivingEntity {
    private final ItemStack[] inventory = new ItemStack[PlayerInventory.INVENTORY_SIZE];

    private int heldSlot = 0;

    public PlaybackPlayerEntity() {
        super(EntityType.MANNEQUIN, UUID.randomUUID());

        setNoGravity(true);
        hasPhysics = false;

        Arrays.fill(inventory, ItemStack.AIR);
    }

    @Override
    protected void movementTick() {
        // Nothing
    }

    public void setItemStack(int slot, ItemStack stack) {
        inventory[slot] = stack;
        if (slot == heldSlot) {
            setItemInMainHand(stack);
            return;
        }

        // Armour and the off hand are inventory slots like any other, but nothing shows them unless
        // they are also worn.
        var equipment = SetItemEvent.equipmentOf(slot);
        if (equipment != null) setEquipment(equipment, stack);
    }

    public void setHeldSlot(int slot) {
        heldSlot = slot;
        setItemInMainHand(inventory[slot]);
    }


}
