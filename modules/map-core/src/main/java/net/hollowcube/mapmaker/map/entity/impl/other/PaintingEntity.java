package net.hollowcube.mapmaker.map.entity.impl.other;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.PaintingMeta;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.RegistryTranscoder;
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
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        var coder = new RegistryTranscoder<>(Transcoder.NBT, MinecraftServer.process());
        tag.put("variant", DataComponents.PAINTING_VARIANT.encode(coder,
                get(DataComponents.PAINTING_VARIANT)).orElseThrow());

        var meta = getEntityMeta();
        tag.putByte("facing", (byte) switch (meta.getOrientation()) {
            case SOUTH -> 0;
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
        });
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        var variantTag = tag.get("variant");
        if (variantTag != null) {
            var coder = new RegistryTranscoder<>(Transcoder.NBT, MinecraftServer.process());
            set(DataComponents.PAINTING_VARIANT, DataComponents.PAINTING_VARIANT.decode(coder, variantTag).orElseThrow());
        }

        var meta = getEntityMeta();
        meta.setOrientation(readOrientation(tag));
    }

    @Override
    public void legacyLoad(@NotNull NetworkBuffer buffer, int version) {
        super.legacyLoad(buffer, version);

        // Orientation is part of the entity object data, not the generic metadata entries
        getEntityMeta().setOrientation(buffer.read(NetworkBuffer.Enum(PaintingMeta.Orientation.class)));
    }

    private @NotNull PaintingMeta.Orientation readOrientation(@NotNull CompoundBinaryTag tag) {
        return switch (tag.getByte("facing", (byte) 0)) {
            case 1 -> PaintingMeta.Orientation.WEST;
            case 2 -> PaintingMeta.Orientation.NORTH;
            case 3 -> PaintingMeta.Orientation.EAST;
            default -> PaintingMeta.Orientation.SOUTH;
        };
    }
}
