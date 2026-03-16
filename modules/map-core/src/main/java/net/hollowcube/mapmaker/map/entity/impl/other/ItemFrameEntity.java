package net.hollowcube.mapmaker.map.entity.impl.other;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.metadata.other.ItemFrameMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.Direction;
import net.minestom.server.utils.Rotation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class ItemFrameEntity extends MapEntity<ItemFrameMeta> {

    public static class Glowing extends ItemFrameEntity {
        public Glowing(@NotNull UUID uuid) {
            super(EntityType.GLOW_ITEM_FRAME, uuid);

            placeSound = SoundEvent.ENTITY_GLOW_ITEM_FRAME_PLACE;
            breakSound = SoundEvent.ENTITY_GLOW_ITEM_FRAME_BREAK;
            addItemSound = SoundEvent.ENTITY_GLOW_ITEM_FRAME_ADD_ITEM;
            removeItemSound = SoundEvent.ENTITY_GLOW_ITEM_FRAME_REMOVE_ITEM;
            rotateItemSound = SoundEvent.ENTITY_GLOW_ITEM_FRAME_ROTATE_ITEM;
        }
    }

    protected SoundEvent placeSound = SoundEvent.ENTITY_ITEM_FRAME_PLACE;
    protected SoundEvent breakSound = SoundEvent.ENTITY_ITEM_FRAME_BREAK;
    protected SoundEvent addItemSound = SoundEvent.ENTITY_ITEM_FRAME_ADD_ITEM;
    protected SoundEvent removeItemSound = SoundEvent.ENTITY_ITEM_FRAME_REMOVE_ITEM;
    protected SoundEvent rotateItemSound = SoundEvent.ENTITY_ITEM_FRAME_ROTATE_ITEM;

    public ItemFrameEntity(@NotNull UUID uuid) {
        this(EntityType.ITEM_FRAME, uuid);
    }

    protected ItemFrameEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);

        hasPhysics = false;
        setNoGravity(true);
    }

    public void playSpawnSound() {
        playSound(placeSound, 1, 1);
    }

    @Override
    public void onBuildLeftClick(@NotNull MapWorld world, @NotNull Player player) {
        var item = getEntityMeta().getItem();
        if (item.isAir()) {
            breakFrame();
        } else {
            removeItem();
        }
    }

    private void breakFrame() {
        playSound(breakSound, 1, 1);
        remove();
    }

    private void removeItem() {
        playSound(removeItemSound, 1, 1);
        getEntityMeta().setItem(ItemStack.AIR);
    }

    @Override
    public void onBuildRightClick(@NotNull MapWorld world, @NotNull Player player, @NotNull PlayerHand hand,
                                  @NotNull Point interactPosition) {
        ItemStack heldItem = player.getItemInMainHand();
        if (heldItem.isAir()) {
            rotate();
        } else {
            addItem(heldItem);
        }
    }

    private void rotate() {
        var meta = getEntityMeta();

        var existingItem = meta.getItem();
        if (existingItem.isAir()) {
            // Don't rotate if there's no item
            return;
        }

        var currentRotation = meta.getRotation();
        var newRotation = currentRotation.rotateClockwise();
        meta.setRotation(newRotation);

        playSound(rotateItemSound, 1, 1);
    }

    private void addItem(@NotNull ItemStack item) {
        playSound(addItemSound, 1, 1);
        getEntityMeta().setItem(item);
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        var meta = getEntityMeta();
        tag.putByte("Facing", (byte) LEGACY_DIRECTIONS.indexOf(meta.getDirection()));
        if (meta.isInvisible()) tag.putBoolean("Invisible", true);
        if (!meta.getItem().isAir()) tag.put("Item", meta.getItem().toItemNBT());

        if (meta.getRotation() != Rotation.NONE) tag.putByte("ItemRotation", (byte) meta.getRotation().ordinal());
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        var meta = getEntityMeta();
        meta.setDirection(readDirection(tag));
        meta.setInvisible(tag.getBoolean("Invisible", false));
        if (tag.get("Item") instanceof CompoundBinaryTag itemTag)
            meta.setItem(ItemStack.fromItemNBT(itemTag));

        meta.setRotation(Rotation.values()[tag.getByte("ItemRotation")]);
    }

    @Override
    public void legacyLoad(@NotNull NetworkBuffer buffer, int version) {
        super.legacyLoad(buffer, version);

        // Orientation is part of the entity object data, not the generic metadata entries
        getEntityMeta().setDirection(LEGACY_DIRECTIONS.get(buffer.read(NetworkBuffer.VAR_INT) % LEGACY_DIRECTIONS.size()));
    }

    private @NotNull Direction readDirection(@NotNull CompoundBinaryTag tag) {
        int id = tag.getByte("Facing", tag.getByte("facing", (byte) 2));
        return LEGACY_DIRECTIONS.get(id % LEGACY_DIRECTIONS.size());
    }

    private static final List<Direction> LEGACY_DIRECTIONS = List.of(
            Direction.DOWN, Direction.UP,
            Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST
    );
}
