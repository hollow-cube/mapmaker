package net.hollowcube.mapmaker.map.biome;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.common.util.dfu.DFU;
import net.hollowcube.terraform.instance.TerraformInstanceBiomes;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.network.packet.server.CachedPacket;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.configuration.RegistryDataPacket;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagWritable;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
 * <p>Protocol biome IDs are inconsistent across runtimes, so only the namespace ID should
 * be used to identify a biome in stored data.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class BiomeContainer implements TerraformInstanceBiomes {
    private static final Logger logger = LoggerFactory.getLogger(BiomeContainer.class);

    private static final Tag<List<BiomeInfo>> TAG = DFU.Tag(BiomeInfo.CODEC.listOf().optionalFieldOf("biomes", List.of()).codec(), "biomes");
    private static final DynamicRegistry.Key<Biome> DEFAULT_BIOME = Biome.PLAINS;

    private static final int FIRST_BIOME_ID;

    static {
        // This is the parent to all biome containers, so load vanilla biomes in it. Then compute the max id to start from.
        var biomeManager = MinecraftServer.getBiomeRegistry();

        var maxId = 0;
        for (var biome : biomeManager.values())
            maxId = Math.max(maxId, biomeManager.getId(biomeManager.getKey(biome)));
        FIRST_BIOME_ID = maxId + 1;
    }

    private final CachedPacket registryDataPacket = new CachedPacket(() -> createRegistryDataPacket(true));

    private final DynamicRegistry<Biome> parent = MinecraftServer.getBiomeRegistry();
    private final List<BiomeInfo> biomes = new ArrayList<>(); // Raw biome data

    private final Map<DynamicRegistry.Key<Biome>, RegisteredBiome> keyToBiome = new HashMap<>();
    private final Int2ObjectMap<RegisteredBiome> idToBiome = new Int2ObjectArrayMap<>();

    private boolean initialized = false;

    @Override
    public @Nullable Biome getBiome(@NotNull DynamicRegistry.Key<Biome> key) {
        var biome = this.keyToBiome.get(key);
        return biome != null ? biome.biome() : this.parent.get(key);
    }

    @Override
    public int getId(@NotNull DynamicRegistry.Key<Biome> key) {
        var biome = this.keyToBiome.get(key);
        return biome != null ? biome.id() : this.parent.getId(key);
    }

    @Override
    public DynamicRegistry.Key<Biome> getKey(int id) {
        return Objects.requireNonNullElse(
                OpUtils.map(this.idToBiome.get(id), RegisteredBiome::key),
                this.parent.getKey(id)
        );
    }

    @Override
    public @NotNull Collection<DynamicRegistry.Key<Biome>> keys() {
        List<DynamicRegistry.Key<Biome>> keys = new ArrayList<>();
        parent.values().forEach(biome -> keys.add(parent.getKey(biome)));
        keys.addAll(keyToBiome.keySet());
        return keys;
    }

    @Override
    public @NotNull Component getName(@NotNull DynamicRegistry.Key<Biome> key) {
        var biome = keyToBiome.get(key);
        if (biome != null) return biome.name();
        var translationKey = "biome.%s.%s".formatted(key.key().namespace(), key.key().value());
        return Component.translatable(translationKey, key.key().asString());
    }

    /**
     * Returns the biome associated with the given id, including from the parent biome manager.
     *
     * <p>Note: only loaded biomes will be returned correctly. If not, the default biome will be returned.</p>
     *
     * @param id The protocol ID of the biome
     * @return The Minestom biome if it exists and is loaded, the default biome otherwise.
     */
    public @NotNull String getLoadedBiomeName(int id) {
        // Try from local biomes first, then from parent
        var biome = idToBiome.get(id);
        if (biome != null) return biome.key().key().asString();
        return Objects.requireNonNullElse(parent.getKey(id), DEFAULT_BIOME).name();
    }

    /**
     * Creates a new unloaded biome. For now it is required to leave and rejoin the world to make the biome load for
     * any players.
     *
     * @return the new biome, or null if the maximum number of biomes has been reached
     */
    public @Nullable BiomeInfo createBiome() {
        if (biomes.size() >= maxSize()) return null;

        var biome = new BiomeInfo();
        biomes.add(biome);
        return biome;
    }

    public boolean hasCustomBiome(@NotNull String name) {
        return this.biomes.stream().anyMatch(b -> b.getName().equals(name));
    }

    public boolean isLoaded(@NotNull BiomeInfo info) {
        return this.keyToBiome.values().stream().anyMatch(b -> b.info().equals(info));
    }

    public int size() {
        return biomes.size();
    }

    public int maxSize() {
        return 15;
    }

    public @NotNull Collection<BiomeInfo> values() {
        return Collections.unmodifiableCollection(biomes);
    }

    public @NotNull List<Biome> loadedBiomes() {
        var allBiomes = new ArrayList<>(this.parent.values());
        for (RegisteredBiome value : this.keyToBiome.values()) {
            allBiomes.add(value.biome());
        }
        return allBiomes;
    }

    public void init(@NotNull TagReadable reader) {
        if (this.initialized) return;
        this.initialized = true;

        var biomes = reader.getTag(TAG);
        if (biomes != null) this.biomes.addAll(biomes);

        int nextId = FIRST_BIOME_ID;
        for (var info : this.biomes) {
            var minestomBiome = info.build();
            if (minestomBiome == null) continue;

            var namespace = Objects.requireNonNull(info.key());
            var key = DynamicRegistry.Key.<Biome>of(namespace);

            var biome = new RegisteredBiome(nextId++, namespace.key(), info, minestomBiome);

            idToBiome.put(biome.id(), biome);
            keyToBiome.put(key, biome);
        }
    }

    public void write(@NotNull TagWritable writer) {
        writer.setTag(TAG, this.biomes);
    }

    public @NotNull SendablePacket registryDataPacket(boolean excludeVanilla) {
        if (excludeVanilla) return registryDataPacket;
        return createRegistryDataPacket(false);
    }

    private @NotNull RegistryDataPacket createRegistryDataPacket(boolean excludeVanilla) {
        List<RegistryDataPacket.Entry> entries = new ArrayList<>();

        // Add parent biomes
        for (var biome : parent.values()) {
            String name = parent.getKey(biome).name();
            if (excludeVanilla) {
                entries.add(new RegistryDataPacket.Entry(name, null));
            } else {
                entries.add(new RegistryDataPacket.Entry(name, Biome.REGISTRY_CODEC.encode(Transcoder.NBT, biome).orElseThrow()));
            }
        }

        // Add overwrite biomes (never vanilla ones so always write entire value)
        for (RegisteredBiome value : this.keyToBiome.values()) {
            entries.add(new RegistryDataPacket.Entry(value.key().key().asString(), Biome.REGISTRY_CODEC.encode(Transcoder.NBT, value.biome()).orElseThrow()));
        }

        return new RegistryDataPacket("minecraft:worldgen/biome", entries);
    }
}
