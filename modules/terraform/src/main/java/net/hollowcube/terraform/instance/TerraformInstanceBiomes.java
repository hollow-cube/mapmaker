package net.hollowcube.terraform.instance;

import net.kyori.adventure.text.Component;
import net.minestom.server.instance.Instance;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.tag.Tag;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface TerraformInstanceBiomes {

    Tag<TerraformInstanceBiomes> BIOMES = Tag.Transient("terraform:biomes");

    @Nullable
    Biome getBiome(RegistryKey<Biome> key);

    int getId(RegistryKey<Biome> key);

    @Nullable RegistryKey<Biome> getKey(int id);

    Collection<RegistryKey<Biome>> keys();

    Component getName(RegistryKey<Biome> key);

    default int size() {
        return keys().size();
    }

    static @Nullable TerraformInstanceBiomes forInstance(Instance instance) {
        return instance.getTag(BIOMES);
    }
}
