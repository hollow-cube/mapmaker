package net.hollowcube.terraform.instance;

import net.kyori.adventure.text.Component;
import net.minestom.server.instance.Instance;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.tag.Tag;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface TerraformInstanceBiomes {

    Tag<TerraformInstanceBiomes> BIOMES = Tag.Transient("terraform:biomes");

    @Nullable
    Biome getBiome(@NotNull RegistryKey<Biome> key);

    int getId(@NotNull RegistryKey<Biome> key);

    @Nullable RegistryKey<Biome> getKey(int id);

    @NotNull Collection<RegistryKey<Biome>> keys();

    @NotNull Component getName(@NotNull RegistryKey<Biome> key);

    default int size() {
        return keys().size();
    }

    static @Nullable TerraformInstanceBiomes forInstance(@NotNull Instance instance) {
        return instance.getTag(BIOMES);
    }
}
