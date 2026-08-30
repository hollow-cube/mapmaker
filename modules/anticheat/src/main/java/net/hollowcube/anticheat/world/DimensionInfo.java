package net.hollowcube.anticheat.world;

/// The only two numbers the world model needs out of a `dimension_type` registry entry: where the
/// bottom section starts and how many sections there are.
///
/// `DimensionType` validates that both are multiples of 16 (`min_y % 16 != 0` and `height % 16 != 0`
/// throw in the 26.2 decompile), so the shifts below are exact.
public record DimensionInfo(String id, int minY, int height) {

    // The vanilla dimension types, used when a `registry_data` entry carries no payload because the
    // client resolved it from a known pack. Values from `DimensionTypes#bootstrap` in the decompile.
    // TODO: we should generate these from registry data.

    public static final DimensionInfo OVERWORLD = new DimensionInfo("minecraft:overworld", -64, 384);
    public static final DimensionInfo OVERWORLD_CAVES = new DimensionInfo("minecraft:overworld_caves", -64, 384);
    public static final DimensionInfo THE_NETHER = new DimensionInfo("minecraft:the_nether", 0, 256);
    public static final DimensionInfo THE_END = new DimensionInfo("minecraft:the_end", 0, 256);

    public int minSectionY() {
        return minY >> 4;
    }

    public int sectionCount() {
        return height >> 4;
    }

    public boolean contains(int y) {
        return y >= minY && y < minY + height;
    }

    /// The index into a chunk's section array for an absolute block y, or -1 outside build height —
    /// which is where `Level#setBlock` gives up too (`isOutsideBuildHeight`).
    public int sectionIndex(int y) {
        return contains(y) ? (y - minY) >> 4 : -1;
    }
}
