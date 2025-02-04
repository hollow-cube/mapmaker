package net.hollowcube.mapmaker.map.biome;

import net.kyori.adventure.text.Component;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.NotNull;

public record RegisteredBiome(
        int id,
        @NotNull NamespaceID namespace,
        @NotNull BiomeInfo info,
        @NotNull Biome biome
) {

    public Component name() {
        return Component.text(this.info.getName());
    }

    public DynamicRegistry.Key<Biome> key() {
        return DynamicRegistry.Key.of(this.namespace);
    }
}
