package dev.hollowcube.replay.event;

import dev.hollowcube.replay.data.ChunkIndex;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/// The slots of an entity's inventory that changed, carried together so that a tick which shuffles
/// several of them costs one event rather than one per slot.
///
/// Slots are Minestom player inventory slots: 0-8 hotbar, 9-35 inventory, 41-44 armour, 45 off
/// hand. Worn equipment is therefore just another slot, and [#slotOf(EquipmentSlot)] maps one
/// across. The main hand is whatever the held slot points at, so it never has a slot of its own.
public record SetItemEvent(int entityId, Map<Integer, ItemStack> items) implements ReplayEvent {
    private static final NetworkBuffer.Type<Map<Integer, CompoundBinaryTag>> RAW_ITEMS_TYPE = NetworkBuffer.VAR_INT
        .mapValue(NetworkBuffer.NBT_COMPOUND);

    public static final ReplayEventCodec<SetItemEvent> CODEC = new ReplayEventCodec<>() {
        @Override
        public void write(NetworkBuffer buffer, SetItemEvent event) {
            var encoded = new LinkedHashMap<Integer, CompoundBinaryTag>(event.items().size());
            for (var entry : event.items().entrySet())
                encoded.put(entry.getKey(), entry.getValue().toItemNBT(ReplayGameData.registries()));
            buffer.write(NetworkBuffer.VAR_INT, event.entityId());
            buffer.write(RAW_ITEMS_TYPE, encoded);
        }

        @Override
        public SetItemEvent read(NetworkBuffer buffer, ChunkIndex chunk) {
            var entityId = buffer.read(NetworkBuffer.VAR_INT);
            var items = buffer.read(RAW_ITEMS_TYPE);
            var decoded = new LinkedHashMap<Integer, ItemStack>(items.size());
            for (var entry : items.entrySet()) {
                var item = ReplayGameData.upgradeItemStack(entry.getValue(), chunk);
                decoded.put(entry.getKey(), ItemStack.fromItemNBT(item, ReplayGameData.registries()));
            }
            return new SetItemEvent(entityId, decoded);
        }
    };

    /// The inventory slot an equipment slot occupies, or -1 for equipment no player inventory has a
    /// slot for: the main hand, which the held slot already names, and the mob-only slots.
    public static int slotOf(EquipmentSlot slot) {
        return switch (slot) {
            case OFF_HAND -> PlayerInventoryUtils.OFFHAND_SLOT;
            case HELMET, CHESTPLATE, LEGGINGS, BOOTS -> slot.armorSlot();
            default -> -1;
        };
    }

    /// The equipment an inventory slot is worn in, or null if wearing it is not what that slot
    /// means.
    public static @Nullable EquipmentSlot equipmentOf(int slot) {
        return switch (slot) {
            case PlayerInventoryUtils.OFFHAND_SLOT -> EquipmentSlot.OFF_HAND;
            case PlayerInventoryUtils.HELMET_SLOT -> EquipmentSlot.HELMET;
            case PlayerInventoryUtils.CHESTPLATE_SLOT -> EquipmentSlot.CHESTPLATE;
            case PlayerInventoryUtils.LEGGINGS_SLOT -> EquipmentSlot.LEGGINGS;
            case PlayerInventoryUtils.BOOTS_SLOT -> EquipmentSlot.BOOTS;
            default -> null;
        };
    }
}
