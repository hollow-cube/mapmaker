package net.hollowcube.mapmaker.map.entity.impl.other;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.object.ObjectTypes;
import net.hollowcube.mapmaker.object.ObjectData;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.PaintingMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class PaintingEntity extends MapEntity {
    private static final DynamicRegistry<PaintingMeta.Variant> PAINTING_REGISTRY = MinecraftServer.getPaintingVariantRegistry();

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
    public CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos spawnPosition) {
        var world = MapWorld.unsafeFromInstance(instance);
//        if (!(world instanceof EditingMapWorld editingWorld)) return CompletableFuture.completedFuture(null);

        boolean added = world.map().addObject(new ObjectData(getUuid().toString(), ObjectTypes.CHECKPOINT_PLATE, spawnPosition));
        if (!added) {
            //todo it needs to be added by a player
//            Audiences.all().sendMessage(Component.translatable("map.ram.usage_exceeded"));
            return CompletableFuture.completedFuture(null);
        }

        return super.setInstance(instance, spawnPosition);
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        var meta = getEntityMeta();
        tag.putString("variant", meta.getVariant().name().toLowerCase(Locale.ROOT));
        tag.putByte("facing", (byte) meta.getOrientation().id());
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        var meta = getEntityMeta();
        var variant = DynamicRegistry.Key.<PaintingMeta.Variant>of(tag.getString("variant", "minecraft:kebab"));
        if (PAINTING_REGISTRY.get(variant) == null) variant = PaintingMeta.Variant.KEBAB;
        meta.setVariant(variant);
        meta.setOrientation(readOrientation(tag));
    }

    @Override
    public void legacyLoad(@NotNull NetworkBuffer buffer, int version) {
        super.legacyLoad(buffer, version);

        // Orientation is part of the entity object data, not the generic metadata entries
        getEntityMeta().setOrientation(buffer.readEnum(PaintingMeta.Orientation.class));
    }

    private @NotNull PaintingMeta.Orientation readOrientation(@NotNull CompoundBinaryTag tag) {
        int id = tag.getByte("facing", (byte) 2);
        for (var orientation : PaintingMeta.Orientation.values()) {
            if (orientation.id() == id) return orientation;
        }
        return PaintingMeta.Orientation.NORTH;
    }
}
