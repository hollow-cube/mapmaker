package net.hollowcube.map.biome;

import net.minestom.server.MinecraftServer;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.utils.validate.Check;
import net.minestom.server.world.biomes.Biome;
import net.minestom.server.world.biomes.BiomeManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Holds a world-local list of biomes, with the ability to reference biomes from the parent (Minestom) biome manager.
 *
 * <p>In practice, this means that the Minestom biome manager will contain vanilla and/or global biomes,
 * this one will contain custom biomes for the map.</p>
 *
 * <p>Note that custom biomes may not be "loaded" which means that they are not registered with the player and
 * <b>may not</b> be set in the world. The world needs to be reloaded for the new biomes to apply or changes
 * to existing ones to take effect</p>
 *
 * <p>Minestom biome IDs ({@link Biome#id()}) are inconsistent across runtimes, so only the namespace ID should
 * be used to identify a biome in stored data.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class BiomeContainer {
    private static final Logger logger = LoggerFactory.getLogger(BiomeContainer.class);

    private final BiomeManager parent = MinecraftServer.getBiomeManager();
    private final List<BiomeInfo> biomes = new ArrayList<>();
    private final Collection<BiomeInfo> unmodifiableBiomes = Collections.unmodifiableCollection(biomes);
    // The Minecraft biomes which were sent to players as they joined.
    // Note: indices correspond to BiomeInfo indices, and null means the biome is not loaded
    private List<Biome> loadedBiomes = null;

    public @Nullable BiomeInfo createBiome() {
        if (biomes.size() >= maxSize()) return null;

        var biome = new BiomeInfo();
        biomes.add(biome);
        return biome;
    }

    public @NotNull Biome getBiome(@NotNull String name) {
        var namespace = NamespaceID.from(name);

        for (var biome : loadedBiomes) {
            if (biome == null) continue;
            if (biome.name().equals(namespace)) return biome;
        }

        return Objects.requireNonNullElse(parent.getByName(namespace), Biome.PLAINS);
    }

    public @NotNull String getBiomeName(int id) {
        for (var biome : loadedBiomes) {
            if (biome == null) continue;
            if (biome.id() == id) return biome.name().asString();
        }

        return Objects.requireNonNullElse(parent.getById(id), Biome.PLAINS).name().asString();
    }

    public int size() {
        return biomes.size();
    }

    public int maxSize() {
        return 5;
    }

    public @NotNull Collection<BiomeInfo> values() {
        return unmodifiableBiomes;
    }

    public NBTCompound toNBT() {
        var allBiomesNbt = new ArrayList<NBTCompound>();

        // Add parent biomes
        for (var biome : parent.unmodifiableCollection()) {
            allBiomesNbt.add(biome.toNbt());
        }
        // Add overwrite biomes
        for (var biome : loadedBiomes) {
            if (biome == null) continue;
            allBiomesNbt.add(biome.toNbt());
        }

        return NBT.Compound(Map.of(
                "type", NBT.String("minecraft:worldgen/biome"),
                "value", NBT.List(NBTType.TAG_Compound, allBiomesNbt)));
    }

    // Serialization

    public void init() {
        Check.stateCondition(loadedBiomes != null, "Biomes should only be initialized once");
        loadedBiomes = new ArrayList<>();

        for (var biome : biomes) {
            var minestomBiome = biome.toMinestomBiome();
            loadedBiomes.add(minestomBiome);
            if (minestomBiome != null) {
                logger.info("Loaded custom biome {} (global id #{})", minestomBiome.name(), minestomBiome.id());
            }
        }
    }

    public void read(@NotNull NetworkBuffer buffer) {
        Check.stateCondition(!biomes.isEmpty(), "Biomes should only be read once");

        this.biomes.addAll(buffer.readCollection(BiomeInfo::new));
    }

    public void write(@NotNull NetworkBuffer buffer) {
        buffer.writeCollection(biomes, (buf, biome) -> biome.write(buf));
    }
}
