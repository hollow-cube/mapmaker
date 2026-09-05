package dev.hollowcube.replay.event;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
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
    private static final NetworkBuffer.Type<Map<Integer, ItemStack>> ITEMS_TYPE = RAW_ITEMS_TYPE
        .transform(SetItemEvent::decodeItems, SetItemEvent::encodeItems);

    public static final NetworkBuffer.Type<SetItemEvent> NETWORK_TYPE = NetworkBufferTemplate.template(
        NetworkBuffer.VAR_INT, SetItemEvent::entityId,
        ITEMS_TYPE, SetItemEvent::items,
        SetItemEvent::new
    );

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

    // Compaction needs the NBT boundary, not item decoding against the worker's game registries.
    static void skip(NetworkBuffer buffer) {
        buffer.read(NetworkBuffer.VAR_INT);
        buffer.read(RAW_ITEMS_TYPE);
    }

    private static Map<Integer, ItemStack> decodeItems(Map<Integer, CompoundBinaryTag> items) {
        var decoded = new LinkedHashMap<Integer, ItemStack>(items.size());
        for (var entry : items.entrySet())
            decoded.put(entry.getKey(), ItemStack.fromItemNBT(entry.getValue(), MinecraftServer.process()));
        return decoded;
    }

    private static Map<Integer, CompoundBinaryTag> encodeItems(Map<Integer, ItemStack> items) {
        var encoded = new LinkedHashMap<Integer, CompoundBinaryTag>(items.size());
        for (var entry : items.entrySet())
            encoded.put(entry.getKey(), entry.getValue().toItemNBT(MinecraftServer.process()));
        return encoded;
    }
}
