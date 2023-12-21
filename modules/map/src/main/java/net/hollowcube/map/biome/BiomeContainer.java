package net.hollowcube.map.biome;

import com.google.protobuf.InvalidProtocolBufferException;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.utils.validate.Check;
import net.minestom.server.world.biomes.Biome;
import net.minestom.server.world.biomes.BiomeEffects;
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
    private final List<Biome> loadedBiomes = new ArrayList<>();
    private boolean initialized = false;

    public @Nullable BiomeInfo createBiome() {
        if (biomes.size() >= maxSize()) return null;

        var biome = new BiomeInfo();
        biomes.add(biome);
        return biome;
    }

    public @NotNull Biome getBiome(@NotNull String name) {
        var namespace = NamespaceID.from(name);

        for (var biome : loadedBiomes) {
            if (biome.name().equals(namespace)) return biome;
        }

        return Objects.requireNonNullElse(parent.getByName(namespace), Biome.PLAINS);
    }

    public @NotNull String getBiomeName(int id) {
        for (var biome : loadedBiomes) {
            if (biome.id() == id) return biome.name().asString();
        }

        return Objects.requireNonNullElse(parent.getById(id), Biome.PLAINS).name().asString();
    }

    public boolean hasCustomBiome(@NotNull String name) {
        return biomes.stream().anyMatch(b -> b.getName().equals(name));
    }

    public int size() {
        return biomes.size();
    }

    public int maxSize() {
        return 15;
    }

    public @NotNull Collection<BiomeInfo> values() {
        return unmodifiableBiomes;
    }

    public @NotNull List<Biome> loadedBiomes() {
        var allBiomes = new ArrayList<>(parent.unmodifiableCollection());
        allBiomes.addAll(loadedBiomes);
        return allBiomes;
    }

    public NBTCompound toNBT() {
        var allBiomesNbt = new ArrayList<NBTCompound>();

        // Add parent biomes
        for (var biome : parent.unmodifiableCollection()) {
            allBiomesNbt.add(biome.toNbt());
        }
        // Add overwrite biomes
        for (var biome : loadedBiomes) {
            allBiomesNbt.add(biome.toNbt());
        }

        return NBT.Compound(Map.of(
                "type", NBT.String("minecraft:worldgen/biome"),
                "value", NBT.List(NBTType.TAG_Compound, allBiomesNbt)));
    }

    // Serialization

    public void init() {
        if (initialized) return;
        initialized = true;

        for (var biome : biomes) {
            var minestomBiome = createMinestomBiome(biome);
            if (minestomBiome == null) continue;

            biome.setMinestomBiome(minestomBiome);
            loadedBiomes.add(minestomBiome);
            logger.info("Loaded custom biome {} (global id #{})", minestomBiome.name(), minestomBiome.id());
        }
    }

    public void read(@NotNull NetworkBuffer buffer) {
        Check.stateCondition(!biomes.isEmpty(), "Biomes should only be read once");

        BiomeProtos.BiomeContainer proto;
        try {
            var bytes = buffer.read(NetworkBuffer.BYTE_ARRAY);
            proto = BiomeProtos.BiomeContainer.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            MinecraftServer.getExceptionManager().handleException(e);
            return;
        }

        for (var biome : proto.getBiomeList()) {
            biomes.add(new BiomeInfo(biome));
        }

        init();
    }

    public void write(@NotNull NetworkBuffer buffer) {
        var builder = BiomeProtos.BiomeContainer.newBuilder();
        for (var biome : biomes) {
            builder = builder.addBiome(biome.toProto());
        }

        buffer.write(NetworkBuffer.BYTE_ARRAY, builder.build().toByteArray());
    }

    private @Nullable Biome createMinestomBiome(@NotNull BiomeInfo bi) {
        if (bi.getName().isEmpty()) return null;

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
                .name(NamespaceID.from("custom", bi.getName()))
                .category(Biome.Category.NONE)
                .temperature(0.8F) // IDK what this affects
                .downfall(0.4F) // IDK what this affects
                .depth(0.125F) // IDK what this affects
                .scale(0.05F) // IDK what this affects
                .effects(effectBuilder.build())
                .build();
    }
}
