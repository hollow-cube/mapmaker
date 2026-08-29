package net.hollowcube.apiworker.index;

import net.hollowcube.datafix.DataFixer;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.polar.PolarDataFixer;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionRegistry;
import net.hollowcube.mapmaker.runtime.parkour.item.checkpoint.CheckpointItems;
import net.hollowcube.polar.PolarReader;
import net.hollowcube.polar.PolarSection;
import net.hollowcube.polar.PolarWorld;
import net.kyori.adventure.nbt.BinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.registry.RegistryTranscoder;

import java.util.Set;

/// Reads stored map worlds and walks their sections.
///
/// Worlds are read straight out of the polar structures rather than through a `MapInstance`,
/// since indexing never needs a live instance and loading one would run the light engine and hold
/// a second copy of the world for every map processed. The tradeoff is that palettes are block
/// *names* rather than resolved `Block`s, so anything needing block properties has to look them
/// up itself.
final class PolarHelper {
    static {
        // Trigger data is stored with the same codecs the runtime uses, and those reach into the
        // registries for attributes, potions and the like, so the server has to be far enough up
        // for MinecraftServer#process to exist. Nothing is bound or ticked by this.
        MinecraftServer.init();
        DataFixer.buildModel();

        // These classes populate registries from their static initialisers, and a codec that
        // resolves against an empty registry decodes to null rather than failing, so an unforced
        // class here shows up as a map that quietly uses no settings or no items.
        MapSettings.ONLY_SPRINT.key();
        ActionRegistry.keys(Action.Type.CHECKPOINT);
        CheckpointItems.keys();
    }

    /// Decodes the mapmaker codecs, which resolve registry keys as they go.
    static final Transcoder<BinaryTag> CODER = new RegistryTranscoder<>(Transcoder.NBT, MinecraftServer.process());

    /// Reads a world, upgrading anything written by an older game version.
    static PolarWorld read(byte[] worldData) {
        return PolarReader.read(worldData, PolarDataFixer.INSTANCE);
    }

    /// Visits every section containing at least one block, in unspecified order.
    ///
    /// Chunks cover the full world height whether or not anything was built there, so the vast
    /// majority of sections in a map are pure air. Those are skipped: a map with 800 chunks has
    /// ~19k sections but only a few hundred worth looking at.
    static void forEachSection(PolarWorld world, SectionVisitor visitor) {
        int minSection = world.minSection();
        for (var chunk : world.chunks()) {
            PolarSection[] sections = chunk.sections();
            for (int i = 0; i < sections.length; i++) {
                PolarSection section = sections[i];
                if (section.isEmpty() || isAirOnly(section)) continue;

                visitor.visit(chunk.x(), minSection + i, chunk.z(), section);
            }
        }
    }

    private static boolean isAirOnly(PolarSection section) {
        String[] palette = section.blockPalette();
        return palette.length == 1 && isAir(palette[0]);
    }

    /// Every block that is really nothing. Polar writes an air-only section as a bare `air` rather
    /// than the namespaced name it gives every other block, and worlds converted from vanilla
    /// carry `cave_air` and `void_air`, which are air with a different name. Counting either of
    /// those as solid inflates the block count, the occupied cells and the extent all at once.
    private static final Set<String> AIR = Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air");

    static boolean isAir(String paletteEntry) {
        return AIR.contains(blockId(paletteEntry));
    }

    /// Reduces a palette entry to the block it names, dropping properties and filling in the
    /// implied namespace, so `oak_stairs[facing=east]` and `minecraft:oak_stairs[facing=west]`
    /// both come back as `minecraft:oak_stairs`.
    ///
    /// Palettes are per-section and hold one entry per block *state*, so a map using stairs in a
    /// few orientations looks far more varied than it is until entries are reduced this way.
    static String blockId(String paletteEntry) {
        int properties = paletteEntry.indexOf('[');
        String name = properties == -1 ? paletteEntry : paletteEntry.substring(0, properties);
        return name.indexOf(':') == -1 ? "minecraft:" + name : name;
    }

    @FunctionalInterface
    interface SectionVisitor {
        /// @param sectionY absolute section index, so world bottom is [PolarWorld#minSection] rather than 0
        void visit(int sectionX, int sectionY, int sectionZ, PolarSection section);
    }

    private PolarHelper() {
    }
}
