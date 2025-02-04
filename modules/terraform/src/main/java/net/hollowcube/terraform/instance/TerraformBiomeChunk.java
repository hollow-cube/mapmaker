package net.hollowcube.terraform.instance;

import net.hollowcube.terraform.util.ProtocolUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.ChunkHack;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.palette.Palette;
import net.minestom.server.network.packet.server.play.ChunkBiomesPacket;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.minestom.server.coordinate.CoordConversion.globalToSectionRelative;

public final class TerraformBiomeChunk {

    private TerraformBiomeChunk() {
    }

    public static @Nullable DynamicRegistry.Key<Biome> getBiome(
            @NotNull Chunk chunk,
            int x, int y, int z
    ) {
        var biomes = TerraformInstanceBiomes.forInstance(chunk.getInstance());
        if (biomes != null) {
            var section = chunk.getSectionAt(y);
            var palette = section.biomePalette();
            var index = palette.get(x / 4, y / 4, z / 4);
            return Objects.requireNonNull(biomes.getKey(index), "no such biome for index: " + index);
        }
        return null;
    }

    public static boolean setBiome(
            @NotNull Chunk chunk,
            int x, int y, int z,
            @NotNull DynamicRegistry.Key<Biome> biome
    ) {
        assert Thread.holdsLock(chunk) : "Chunk must be locked before access";
        ChunkHack.invalidateChunk(chunk);

        var biomes = TerraformInstanceBiomes.forInstance(chunk.getInstance());
        if (biomes != null) {
            var section = chunk.getSectionAt(y);
            var palette = section.biomePalette();
            var index = biomes.getId(biome);

            if (index != -1) {
                palette.set(
                        globalToSectionRelative(x) / 4,
                        globalToSectionRelative(y) / 4,
                        globalToSectionRelative(z) / 4,
                        index
                );
                return true;
            }
        }
        return false;
    }

    public static void fillBiome(
            @NotNull Chunk chunk,
            @NotNull DynamicRegistry.Key<Biome> biome
    ) {
        assert Thread.holdsLock(chunk) : "Chunk must be locked before access";
        ChunkHack.invalidateChunk(chunk);

        var biomes = TerraformInstanceBiomes.forInstance(chunk.getInstance());
        int id = biomes != null ? biomes.getId(biome) : -1;
        if (id == -1) id = MinecraftServer.getBiomeRegistry().getId(biome);
        if (id == -1) throw new IllegalStateException("Biome has not been registered: " + biome.namespace());

        for (Section section : chunk.getSections()) {
            section.biomePalette().fill(id);
        }
    }

    public static void sendBiomeUpdates(@NotNull List<Chunk> chunks) {
        List<ChunkBiomesPacket.ChunkBiomeData> data = new ArrayList<>();
        for (Chunk chunk : chunks) {
            data.add(new ChunkBiomesPacket.ChunkBiomeData(
                    chunk.getChunkX(), chunk.getChunkZ(),
                    ProtocolUtil.makeArray(1024, buffer -> {
                        for (Section section : chunk.getSections()) {
                            Palette.BIOME_SERIALIZER.write(buffer, section.biomePalette());
                        }
                    })
            ));
        }

        var packet = new ChunkBiomesPacket(data);

        Set<UUID> hasSent = new HashSet<>();

        for (Chunk chunk : chunks) {
            for (var viewer : chunk.getViewers()) {
                if (!hasSent.add(viewer.getUuid())) continue;
                viewer.sendPacket(packet);
            }
        }
    }
}
