package net.hollowcube.mapmaker.map.biome;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.NotNull;

public record RegisteredBiome(
        int id,
        @NotNull Key innerKey,
        @NotNull BiomeInfo info,
        @NotNull Biome biome
) {

    public Component name() {
        return Component.text(this.info.getName());
    }

    public RegistryKey<Biome> key() {
        return RegistryKey.unsafeOf(this.innerKey);
    }
}
