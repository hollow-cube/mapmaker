package net.hollowcube.mapmaker.runtime.parkour.item.checkpoint;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.registry.DynamicRegistry;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class CheckpointItems {
    private static final DynamicRegistry<StructCodec<? extends CheckpointItem>> REGISTRY = DynamicRegistry.create(Key.key("mapmaker:checkpoint_item"));
    private static final List<Key> KEYS = new ArrayList<>();

    public static final StructCodec<CheckpointItem> CODEC = Codec.RegistryTaggedUnion(_ -> REGISTRY, CheckpointItem::codec, "item");

    public static Key getKey(CheckpointItem item) {
        var registryKey = REGISTRY.getKey(item.codec());
        return registryKey.key();
    }

    public static CheckpointItem createDefault(Key key) {
        var codec = REGISTRY.get(key);
        if (codec == null)
            throw new IllegalArgumentException("No checkpoint item found for key: " + key);
        return codec.decode(Transcoder.NBT, CompoundBinaryTag.empty()).orElseThrow();
    }

    public static List<Key> keys() {
        return KEYS;
    }

    static {
        register(EnderPearlCheckpointItem.ID, EnderPearlCheckpointItem.CODEC);
        register(FireworkRocketCheckpointItem.ID, FireworkRocketCheckpointItem.CODEC);
        register(TridentCheckpointItem.ID, TridentCheckpointItem.CODEC);
        register(WindChargeCheckpointItem.ID, WindChargeCheckpointItem.CODEC);
        register(BlockCheckpointItem.ID, BlockCheckpointItem.CODEC);
    }

    private static <T extends CheckpointItem> void register(Key id, StructCodec<T> codec) {
        REGISTRY.register(id, codec);
        KEYS.add(id);
    }
}
