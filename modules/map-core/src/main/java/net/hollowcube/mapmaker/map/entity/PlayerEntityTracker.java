package net.hollowcube.mapmaker.map.entity;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.terraform.compat.axiom.util.NbtUtil;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerEntityTracker {
    private static final Tag<PlayerEntityTracker> TAG = Tag.Transient("player_entity_tracker");

    public static @Nullable PlayerEntityTracker forPlayerOptional(@NotNull Player player) {
        var existing = player.getTag(TAG);
        if (existing != null) {
            if (existing.instance.equals(player.getInstance())) {
                return existing;
            }

            // Sanity check but it was old, so remove it.
            existing.clear();
            player.removeTag(TAG);
        }
        return null;
    }

    public static @NotNull PlayerEntityTracker forPlayer(@NotNull Player player) {
        var existing = forPlayerOptional(player);
        if (existing != null) return existing;

        var manager = new PlayerEntityTracker(player);
        player.setTag(TAG, manager);
        return manager;
    }

    private final MapPlayer player;
    private final Instance instance;

    private final List<MapEntity> entities = new ArrayList<>();

    private PlayerEntityTracker(Player player) {
        this.player = (MapPlayer) player;
        this.instance = player.getInstance();
    }

    public void add(Entity entity) {
        if (!(entity instanceof MapEntity mapEntity))
            throw new IllegalArgumentException("Only MapEntity instances can be added to PlayerEntityTracker");

        entities.add(mapEntity);
        player.addOwnedEntity(mapEntity);
    }

    public void clear() {

    }

    public List<Codec.RawValue> save() {
        var list = new ArrayList<Codec.RawValue>();
        var iter = entities.iterator();
        while (iter.hasNext()) {
            var entity = iter.next();
            if (entity.isRemoved()) iter.remove();

            var tag = CompoundBinaryTag.builder();
            entity.writeData(tag);
            list.add(Codec.RawValue.of(Transcoder.NBT, tag.build()));
        }
        return list;
    }

    public void load(List<Codec.RawValue> data) {
        for (var value : data) {
            var tag = (CompoundBinaryTag) value.convertTo(Transcoder.NBT).orElseThrow();
            var entityType = EntityType.fromKey(tag.getString("id"));
            var entity = (MapEntity) MapEntityType.create(entityType, UUID.randomUUID());
            entity.readData(tag);

            var spawnPos = OpUtils.or(NbtUtil.readSpawnPosition(tag), () -> Pos.ZERO);
            entity.setInstance(instance, spawnPos);
            add(entity);
        }
    }

}
