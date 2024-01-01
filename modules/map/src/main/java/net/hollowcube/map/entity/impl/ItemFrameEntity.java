package net.hollowcube.map.entity.impl;

import net.hollowcube.map.entity.MapEntity;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.ItemFrameMeta;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class ItemFrameEntity extends MapEntity {

    protected SoundEvent placeSound = SoundEvent.ENTITY_ITEM_FRAME_PLACE;
    protected SoundEvent breakSound = SoundEvent.ENTITY_ITEM_FRAME_BREAK;
    protected SoundEvent addItemSound = SoundEvent.ENTITY_ITEM_FRAME_ADD_ITEM;
    protected SoundEvent removeItemSound = SoundEvent.ENTITY_ITEM_FRAME_REMOVE_ITEM;
    protected SoundEvent rotateItemSound = SoundEvent.ENTITY_ITEM_FRAME_ROTATE_ITEM;

    public ItemFrameEntity(@NotNull UUID uuid) {
        super(EntityType.ITEM_FRAME, uuid);
    }

    protected ItemFrameEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

    public void playSpawnSound() {
        playSound(placeSound, 1, 1);
    }

    @Override
    public void onBuildLeftClick(@NotNull MapWorld world, @NotNull Player player) {
        playSound(breakSound, 1, 1);
        remove();
    }

    @Override
    public @NotNull ItemFrameMeta getEntityMeta() {
        return (ItemFrameMeta) super.getEntityMeta();
    }

    @Override
    public void save(@NotNull NetworkBuffer buffer) {
        super.save(buffer);

        // Orientation is part of the entity object data, not the generic metadata entries
        buffer.writeEnum(ItemFrameMeta.Orientation.class, getEntityMeta().getOrientation());
    }

    @Override
    public void load(@NotNull NetworkBuffer buffer, int version) {
        super.load(buffer, version);

        // Orientation is part of the entity object data, not the generic metadata entries
        getEntityMeta().setOrientation(buffer.readEnum(ItemFrameMeta.Orientation.class));
    }

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
}
