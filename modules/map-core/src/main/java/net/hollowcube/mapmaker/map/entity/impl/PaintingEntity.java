package net.hollowcube.mapmaker.map.entity.impl;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.PaintingMeta;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class PaintingEntity extends MapEntity {
    protected SoundEvent placeSound = SoundEvent.ENTITY_PAINTING_PLACE;
    protected SoundEvent breakSound = SoundEvent.ENTITY_PAINTING_BREAK;

    public PaintingEntity(@NotNull UUID uuid) {
        this(EntityType.PAINTING, uuid);
    }

    protected PaintingEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);

        hasPhysics = false;
        setNoGravity(true);
    }

    public void playSpawnSound() {
        playSound(placeSound, 1, 1);
    }

    @Override
    public void onBuildLeftClick(@NotNull MapWorld world, @NotNull Player player) {
        breakPainting();
    }

    private void breakPainting() {
        playSound(breakSound, 1, 1);
        remove();
    }

    @Override
    public @NotNull PaintingMeta getEntityMeta() {
        return (PaintingMeta) super.getEntityMeta();
    }

    @Override
    public void save(@NotNull NetworkBuffer buffer) {
        super.save(buffer);

        // Orientation is part of the entity object data, not the generic metadata entries
        buffer.writeEnum(PaintingMeta.Orientation.class, getEntityMeta().getOrientation());
    }

    @Override
    public void load(@NotNull NetworkBuffer buffer, int version) {
        super.load(buffer, version);

        // Orientation is part of the entity object data, not the generic metadata entries
        getEntityMeta().setOrientation(buffer.readEnum(PaintingMeta.Orientation.class));
    }
}
