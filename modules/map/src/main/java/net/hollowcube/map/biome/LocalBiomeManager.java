package net.hollowcube.map.biome;

import net.minestom.server.MinecraftServer;
import net.minestom.server.world.biomes.Biome;
import net.minestom.server.world.biomes.BiomeManager;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalBiomeManager {
    private final BiomeManager parent = MinecraftServer.getBiomeManager();
    private final Map<Integer, Biome> biomes = new ConcurrentHashMap<>();

    public void addBiome(@NotNull Biome biome) {
        biomes.put(biome.id(), biome);
    }

    public @NotNull List<Biome> values() {
        var allBiomes = new ArrayList<Biome>();
        allBiomes.addAll(parent.unmodifiableCollection());
        allBiomes.addAll(biomes.values());
        return Collections.unmodifiableList(allBiomes);
    }

    public NBTCompound toNBT() {
        var allBiomesNbt = new ArrayList<NBTCompound>();

        // Add parent biomes
        for (var biome : parent.unmodifiableCollection()) {
            allBiomesNbt.add(biome.toNbt());
        }
        // Add overwrite biomes
        for (var biome : biomes.values()) {
            allBiomesNbt.add(biome.toNbt());
        }

        return NBT.Compound(Map.of(
                "type", NBT.String("minecraft:worldgen/biome"),
                "value", NBT.List(NBTType.TAG_Compound, allBiomesNbt)));
    }
}
