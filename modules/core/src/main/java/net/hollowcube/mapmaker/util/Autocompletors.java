package net.hollowcube.mapmaker.util;

import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.sound.BuiltinSoundEvent;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public final class Autocompletors {

    private static final TokenIndex<Material> materials = new TokenIndex<>(
        Material.values(), m -> m.key().value());
    private static final TokenIndex<Block> fullBlocks = new TokenIndex<>(
        ItemUtils.PLACEABLE_ON_BLOCKS, b -> b.key().value());
    private static final TokenIndex<BuiltinSoundEvent> sounds = new TokenIndex<>(
        SoundEvent.values().stream()
            .filter(sound -> sound != SoundEvent.INTENTIONALLY_EMPTY)
            .map(BuiltinSoundEvent.class::cast)
            .toList(),
        s -> s.key().value());

    public static @NotNull List<Material> searchMaterials(@NotNull String query, int limit, Predicate<Material> predicate) {
        return materials.search(query, limit, predicate);
    }

    public static @NotNull List<Block> searchBlocks(@NotNull String query, int limit) {
        return fullBlocks.search(query, limit, _ -> true);
    }

    public static @NotNull List<Block> searchBlocks(@NotNull String query, int limit, Predicate<Block> predicate) {
        return fullBlocks.search(query, limit, predicate);
    }

    public static @NotNull List<BuiltinSoundEvent> searchSounds(@NotNull String query, int limit) {
        return sounds.search(query, limit, _ -> true);
    }
}
