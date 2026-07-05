package net.hollowcube.mapmaker.runtime.parkour.action;

import net.hollowcube.mapmaker.map.entity.OwnedEntity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public record OwnedEntityList(List<Saved> entities) {
    private static final int MAX_SAVED = 64;

    public static @Nullable OwnedEntityList save(Collection<? extends OwnedEntity> snapshots) {
        var saved = new ArrayList<Saved>();
        for (var snapshot : snapshots) {
            saved.add(new Saved(snapshot.ownedEntityType(), snapshot.saveOwnedEntityData()));
        }
        return saved.isEmpty() ? null : new OwnedEntityList(saved);
    }

    public record Saved(Key kind, CompoundBinaryTag nbt) {
        public static final StructCodec<Saved> CODEC = StructCodec.struct(
            "kind", Codec.KEY, Saved::kind,
            "nbt", Codec.NBT_COMPOUND, Saved::nbt,
            Saved::new);
    }

    public static final Codec<OwnedEntityList> CODEC = Saved.CODEC.list(MAX_SAVED)
        .transform(OwnedEntityList::new, OwnedEntityList::entities);

    public OwnedEntityList {
        entities = List.copyOf(entities);
    }

}
