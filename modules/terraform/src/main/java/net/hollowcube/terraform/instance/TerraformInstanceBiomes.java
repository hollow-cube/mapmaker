package net.hollowcube.terraform.instance;

import net.kyori.adventure.text.Component;
import net.minestom.server.instance.Instance;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.tag.Tag;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface TerraformInstanceBiomes {

    Tag<TerraformInstanceBiomes> BIOMES = Tag.Transient("terraform:biomes");

    @Nullable
    Biome getBiome(@NotNull DynamicRegistry.Key<Biome> key);

    int getId(@NotNull DynamicRegistry.Key<Biome> key);

    @Nullable DynamicRegistry.Key<Biome> getKey(int id);

    @NotNull Collection<DynamicRegistry.Key<Biome>> keys();

    @NotNull Component getName(@NotNull DynamicRegistry.Key<Biome> key);

    static @Nullable TerraformInstanceBiomes forInstance(@NotNull Instance instance) {
        return instance.getTag(BIOMES);
    }
}
