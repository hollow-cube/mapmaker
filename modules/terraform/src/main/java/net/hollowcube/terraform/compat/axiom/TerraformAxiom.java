package net.hollowcube.terraform.compat.axiom;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.hollowcube.terraform.compat.axiom.packet.client.AxiomClientSetBufferPacket;
import net.hollowcube.terraform.util.PaletteUtil;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.MultiBlockChangePacket;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("UnstableApiUsage")
public class TerraformAxiom {
    private static final Logger logger = LoggerFactory.getLogger(TerraformAxiom.class);


    @Blocking
    public static void applyBlockBuffer(@NotNull Player player, @NotNull AxiomClientSetBufferPacket.BlockBuffer buffer) {
        var instance = player.getInstance();

        int[] paletteData = new int[4096]; // Reused buffer
        var sectionChangeCache = new LongArrayList();
        for (var sectionUpdate : buffer.updates()) {
            int chunkX = PaletteUtil.unpackX(sectionUpdate.index());
            int sectionY = PaletteUtil.unpackY(sectionUpdate.index());
            int chunkZ = PaletteUtil.unpackZ(sectionUpdate.index());

            // Ensure chunk is loaded
            var chunk = instance.getChunk(chunkX, chunkZ);
            if (chunk == null) {
                logger.warn("Received block buffer for unloaded chunk ({}, {}) from {}",
                        chunkX, chunkZ, player.getUuid());
                continue;
            }

            // Apply the changes to the section and queue the update for the viewers
            var section = chunk.getSection(sectionY);
            sectionChangeCache.clear();
            synchronized (chunk) {
                //todo optimize this, fixed palette can be a single full update
                sectionUpdate.palette().read(paletteData); // Read palette into shared buffer

                var indexCache = new AtomicInteger(0);
                section.blockPalette().getAll((sx, sy, sz, stateId) -> {
                    var paletteIndex = indexCache.getAndIncrement();
                    var newBlockState = paletteData[paletteIndex];

                    if (newBlockState == Axiom.EMPTY_BLOCK_STATE) {
                        paletteData[paletteIndex] = stateId;
                    } else {
                        sectionChangeCache.add(((long) newBlockState << 12) | ((long) sx << 8 | (long) sz << 4 | sy));
                    }
                });

                indexCache.set(0);
                section.blockPalette().setAll((x, y, z) -> paletteData[indexCache.getAndIncrement()]);
            }

            var updateIndex = (((long) chunkX & 0x3FFFFF) << 42) | ((long) sectionY & 0xFFFFF) | (((long) chunkZ & 0x3FFFFF) << 20);
            var packet = new MultiBlockChangePacket(updateIndex, sectionChangeCache.toLongArray());
            chunk.sendPacketsToViewers(packet);
        }

        logger.warn("Received block buffer with too many changes ({} remaining) from {}",
                buffer.overflow(), player.getUuid());
    }

    @Blocking
    public static void handleSetBiomeBuffer(@NotNull Player player, @NotNull AxiomClientSetBufferPacket.BiomeBuffer buffer) {
        logger.warn("Do not know how to apply biome buffer!");
        //todo
    }

}
