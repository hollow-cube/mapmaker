package net.hollowcube.anticheat.protocol;

import org.jetbrains.annotations.Nullable;

/// `CommonPlayerSpawnInfo`, shared by `login` and `respawn`. [#dimensionTypeId()] indexes the
/// `dimension_type` registry sent during configuration, which is where the world model gets its
/// min y and height.
public record CommonPlayerSpawnInfo(
    int dimensionTypeId,
    String dimension,
    long seed,
    int gameType,
    byte previousGameType,
    boolean isDebug,
    boolean isFlat,
    @Nullable GlobalPos lastDeathLocation,
    int portalCooldown,
    int seaLevel
) {

    public static CommonPlayerSpawnInfo decode(ByteReader reader) {
        return new CommonPlayerSpawnInfo(
            reader.varInt(),
            reader.utf(),
            reader.i64(),
            reader.u8(),
            reader.i8(),
            reader.bool(),
            reader.bool(),
            reader.optional(GlobalPos::decode),
            reader.varInt(),
            reader.varInt());
    }

    public void encode(ByteWriter writer) {
        writer.varInt(dimensionTypeId).utf(dimension).i64(seed)
            .u8(gameType).u8(previousGameType)
            .bool(isDebug).bool(isFlat)
            .optional(lastDeathLocation, (out, pos) -> pos.encode(out))
            .varInt(portalCooldown).varInt(seaLevel);
    }

    /// A dimension plus a `BlockPos#asLong`.
    public record GlobalPos(String dimension, long packedPos) {

        public static GlobalPos decode(ByteReader reader) {
            return new GlobalPos(reader.utf(), reader.blockPos());
        }

        public void encode(ByteWriter writer) {
            writer.utf(dimension).blockPos(packedPos);
        }
    }
}
