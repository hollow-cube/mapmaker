package net.hollowcube.mapmaker.map.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.mapmaker.util.dfu.DFU;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagWritable;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.biomes.Biome;
import net.minestom.server.world.biomes.BiomeEffects;
import net.minestom.server.world.biomes.BiomeManager;
import net.minestom.server.world.biomes.VanillaBiome;
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
 * <p>Protocol biome IDs are inconsistent across runtimes, so only the namespace ID should
 * be used to identify a biome in stored data.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class BiomeContainer {
    private static final Logger logger = LoggerFactory.getLogger(BiomeContainer.class);

    // Exists because DFU.Tag requires an object, not a list.
    private record TagWrapper(List<BiomeInfo> biomes) {
    }

    private static final Tag<TagWrapper> TAG = DFU.Tag(RecordCodecBuilder.create(i -> i.group(
            Codec.list(BiomeInfo.CODEC).optionalFieldOf("biomes", List.of()).forGetter(TagWrapper::biomes)
    ).apply(i, TagWrapper::new)), "biomes");
    private static final Biome DEFAULT_BIOME = VanillaBiome.PLAINS;

    private static final int FIRST_BIOME_ID;

    static {
        // This is the parent to all biome containers, so load vanilla biomes in it. Then compute the max id to start from.
        var biomeManager = MinecraftServer.getBiomeManager();
        biomeManager.loadVanillaBiomes();

        var maxId = 0;
        for (var biome : biomeManager.unmodifiableCollection())
            maxId = Math.max(maxId, biomeManager.getId(biome));
        FIRST_BIOME_ID = maxId + 1;
    }

    private final BiomeManager parent = MinecraftServer.getBiomeManager();
    private final List<BiomeInfo> biomes = new ArrayList<>(); // Raw biome data
    private final Int2ObjectMap<Biome> loadedBiomes = new Int2ObjectArrayMap<>(); // Custom biomes that are loaded (ie have a Minestom biome)
    private boolean initialized = false;

    /**
     * Returns the biome associated with the given name, including from the parent biome manager.
     *
     * <p>Note: only loaded biomes will be returned correctly. If not, the default biome will be returned.</p>
     *
     * @param name The name of the biome
     * @return The Minestom biome if it exists and is loaded, the default biome otherwise.
     */
    public @NotNull Biome getLoadedBiome(@NotNull String name) {
        var namespace = NamespaceID.from(name);

        for (var biome : loadedBiomes.values()) {
            if (biome.namespace().equals(namespace)) return biome;
        }

        return Objects.requireNonNullElse(parent.getByName(namespace), DEFAULT_BIOME);
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
        return Objects.requireNonNullElseGet(
                loadedBiomes.get(id), // Try from local biomes first, then from parent
                () -> Objects.requireNonNullElse(parent.getById(id), DEFAULT_BIOME)
        ).name();
    }

    public int getLoadedBiomeId(@NotNull Biome biome) {
        for (var entry : loadedBiomes.int2ObjectEntrySet()) {
            if (entry.getValue().equals(biome)) return entry.getIntKey();
        }
        return parent.getId(biome);
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
        return biomes.stream().anyMatch(b -> b.getName().equals(name));
    }

    public boolean isLoaded(@NotNull BiomeInfo info) {
        return loadedBiomes.values().stream().anyMatch(b -> b.namespace().equals(info.namespace()));
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
        var allBiomes = new ArrayList<>(parent.unmodifiableCollection());
        allBiomes.addAll(loadedBiomes.values());
        return allBiomes;
    }

    public NBTCompound toNBT() {
        var allBiomesNbt = new ArrayList<NBTCompound>();
        //todo should probably cache this nbt

        // Add parent biomes
        for (var biome : parent.unmodifiableCollection()) {
            allBiomesNbt.add(NBT.Compound(Map.of(
                    "id", NBT.Int(getLoadedBiomeId(biome)),
                    "name", NBT.String(biome.namespace().toString()),
                    "element", biome.toNbt()
            )));
        }
        // Add overwrite biomes
        for (var biome : loadedBiomes.values()) {
            allBiomesNbt.add(NBT.Compound(Map.of(
                    "id", NBT.Int(getLoadedBiomeId(biome)),
                    "name", NBT.String(biome.namespace().toString()),
                    "element", biome.toNbt()
            )));
        }

        return NBT.Compound(Map.of(
                "type", NBT.String("minecraft:worldgen/biome"),
                "value", NBT.List(NBTType.TAG_Compound, allBiomesNbt)));
    }

    public void init(@NotNull TagReadable dataReader) {
        if (initialized) return;
        initialized = true;

        var tag = dataReader.getTag(TAG);
        if (tag != null) biomes.addAll(tag.biomes());

        int nextId = FIRST_BIOME_ID;
        for (var biome : biomes) {
            var minestomBiome = createMinestomBiome(biome);
            if (minestomBiome == null) continue;

            var protocolId = nextId++;
            loadedBiomes.put(protocolId, minestomBiome);
            logger.info("Loaded custom biome {} (id #{})", minestomBiome.name(), protocolId);
        }
    }

    public void write(@NotNull TagWritable dataWriter) {
        dataWriter.setTag(TAG, new TagWrapper(biomes));
    }

    private @Nullable Biome createMinestomBiome(@NotNull BiomeInfo bi) {
        var namespace = bi.namespace();
        if (namespace == null) return null;

        var effectBuilder = BiomeEffects.builder()
                .skyColor(TextColor.fromCSSHexString(bi.getSkyColor()).value())
                .fogColor(TextColor.fromCSSHexString(bi.getFogColor()).value())
                .waterColor(TextColor.fromCSSHexString(bi.getWaterColor()).value())
                .waterFogColor(TextColor.fromCSSHexString(bi.getWaterFogColor()).value());
        if (bi.getGrassColor() != null) {
            var color = TextColor.fromCSSHexString(bi.getGrassColor());
            if (color != null) effectBuilder.grassColor(color.value());
        }
        if (bi.getFoliageColor() != null) {
            var color = TextColor.fromCSSHexString(bi.getFoliageColor());
            if (color != null) effectBuilder.foliageColor(color.value());
        }

        return Biome.builder()
                .name(namespace)
                .temperature(0.8F) // IDK what this affects
                .downfall(0.4F) // IDK what this affects
                .depth(0.125F) // IDK what this affects
                .scale(0.05F) // IDK what this affects
                .effects(effectBuilder.build())
                .build();
    }
}
