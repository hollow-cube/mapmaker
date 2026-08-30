package net.hollowcube.anticheat.world;

import net.hollowcube.anticheat.protocol.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/// Hand-built 776 packets for the world model tests.
final class TestPackets {

    static final String OVERWORLD = "minecraft:overworld";
    static final String THE_NETHER = "minecraft:the_nether";

    static S2CLogin.V776 login(String dimension, int dimensionTypeId, int chunkRadius) {
        return new S2CLogin.V776(1, false, List.of(OVERWORLD), 20, chunkRadius, 12,
            false, true, false, spawnInfo(dimension, dimensionTypeId), false, false);
    }

    static S2CRespawn.V776 respawn(String dimension, int dimensionTypeId) {
        return new S2CRespawn.V776(spawnInfo(dimension, dimensionTypeId), (byte) 0);
    }

    static CommonPlayerSpawnInfo spawnInfo(String dimension, int dimensionTypeId) {
        return new CommonPlayerSpawnInfo(dimensionTypeId, dimension, 0L, 0, (byte) -1, false, false, null, 0, 63);
    }

    /// A chunk of `sectionCount` single-value air sections, which is the smallest legal shape.
    static S2CLevelChunkWithLight.V776 chunk(int chunkX, int chunkZ, int sectionCount) {
        var sections = new ArrayList<Section>(sectionCount);
        for (int i = 0; i < sectionCount; i++) sections.add(airSection());
        return new S2CLevelChunkWithLight.V776(chunkX, chunkZ, heightmaps(), List.copyOf(sections),
            ByteSlice.of(new byte[]{0, 0, 0, 0}));
    }

    static Section airSection() {
        return new Section(0, 0, 0, new int[]{0}, new long[0], biomes());
    }

    /// A four-bit section whose whole storage holds palette index 0.
    static Section palettedSection(int... palette) {
        return new Section(4096, 0, 4, palette.clone(),
            new long[Section.longCount(4, Section.BLOCK_ENTRY_COUNT)], biomes());
    }

    static ByteSlice heightmaps() {
        return ByteSlice.of(new ByteWriter().varInt(0).toByteArray());
    }

    static byte[] biomes() {
        return new ByteWriter().u8(0).varInt(0).toByteArray();
    }

    /// A `dimension_type` registry whose entries carry real network NBT, so the min y and height
    /// have to come back out of it the way they do off the wire.
    static S2CRegistryData.V776 dimensionTypes(DimensionInfo... dimensions) {
        var entries = new ArrayList<S2CRegistryData.Entry>(dimensions.length);
        for (DimensionInfo dimension : dimensions)
            entries.add(new S2CRegistryData.Entry(dimension.id(), dimensionNbt(dimension)));
        return new S2CRegistryData.V776("minecraft:dimension_type", List.copyOf(entries));
    }

    /// A compound with the two ints the model wants, surrounded by the tag types it has to skip.
    static byte[] dimensionNbt(DimensionInfo dimension) {
        var writer = new ByteWriter();
        writer.u8(10); // root compound, no name in the network form
        writer.u8(1).bytes(name("has_skylight")).u8(1);
        writer.u8(6).bytes(name("coordinate_scale")).f64(1.0);
        writer.u8(8).bytes(name("effects")).i16(9).bytes("overworld".getBytes(StandardCharsets.UTF_8));
        writer.u8(3).bytes(name("min_y")).i32(dimension.minY());
        writer.u8(9).bytes(name("monster_spawn_light_level")).u8(3).i32(2).i32(0).i32(7);
        writer.u8(3).bytes(name("height")).i32(dimension.height());
        writer.u8(10).bytes(name("nested"));
        writer.u8(11).bytes(name("ints")).i32(2).i32(1).i32(2);
        writer.u8(0); // end of nested
        writer.u8(3).bytes(name("logical_height")).i32(dimension.height());
        writer.u8(0); // end of root
        return writer.toByteArray();
    }

    private static byte[] name(String value) {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        return new ByteWriter().i16(encoded.length).bytes(encoded).toByteArray();
    }

    private TestPackets() {}
}
