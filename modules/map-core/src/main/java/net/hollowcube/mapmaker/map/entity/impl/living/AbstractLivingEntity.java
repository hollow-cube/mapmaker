package net.hollowcube.mapmaker.map.entity.impl.living;

import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.inventory.EquipmentHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.LazyPacket;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;

import static net.hollowcube.mapmaker.map.util.NbtUtilV2.*;

/**
 * Represents a LivingEntity in the game. This class basically reimplements the
 * {@link net.minestom.server.entity.LivingEntity} from Minestom, although this
 * implementation is <b>not</b> (necessarily) thread-safe.
 */
@SuppressWarnings("UnstableApiUsage")
public class AbstractLivingEntity extends MapEntity implements EquipmentHandler {
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{
            EquipmentSlot.BOOTS, EquipmentSlot.LEGGINGS,
            EquipmentSlot.CHESTPLATE, EquipmentSlot.HELMET
    };
    private static final EquipmentSlot[] HAND_SLOTS = new EquipmentSlot[]{
            EquipmentSlot.MAIN_HAND, EquipmentSlot.OFF_HAND
    };

    // Indices are the ordinal of the EquipmentSlot enum.
    private final ItemStack[] equipment = new ItemStack[EquipmentSlot.values().length];

    protected AbstractLivingEntity(@NotNull EntityType entityType) {
        this(entityType, UUID.randomUUID());
    }

    protected AbstractLivingEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);

        Arrays.fill(equipment, ItemStack.AIR);
    }

    @Override
    public @NotNull LivingEntityMeta getEntityMeta() {
        return (LivingEntityMeta) super.getEntityMeta();
    }

    // There is a lot of Mojang serialized fields missing here, may need to add more later.

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        final LivingEntityMeta meta = getEntityMeta();
        tag.put("ArmorItems", writeListIndexed(ARMOR_SLOTS, slot -> writeItemStack(getEquipment(slot))));
        tag.put("HandItems", writeListIndexed(HAND_SLOTS, slot -> writeItemStack(getEquipment(slot))));

    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        final LivingEntityMeta meta = getEntityMeta();
        readListIndexed(tag.get("ArmorItems"), ARMOR_SLOTS, (slot, itemTag) ->
                setEquipment(slot, readItemStack(itemTag)));
        readListIndexed(tag.get("HandItems"), HAND_SLOTS, (slot, itemTag) ->
                setEquipment(slot, readItemStack(itemTag)));

    }

    // EquipmentHandler impl


    @Override
    public @NotNull ItemStack getEquipment(@NotNull EquipmentSlot slot) {
        return equipment[slot.ordinal()];
    }

    @Override
    public void setEquipment(@NotNull EquipmentSlot slot, @NotNull ItemStack itemStack) {
        equipment[slot.ordinal()] = itemStack;
        sendPacketToViewersAndSelf(new LazyPacket(this::getEquipmentsPacket));
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        super.updateNewViewer(player);
        player.sendPacket(new LazyPacket(this::getEquipmentsPacket));
//        player.sendPacket(new LazyPacket(this::getAttributesPacket));
    }

}
