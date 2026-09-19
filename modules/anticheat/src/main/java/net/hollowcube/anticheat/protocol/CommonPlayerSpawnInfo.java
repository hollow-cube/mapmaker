package net.hollowcube.anticheat.protocol;

import org.jetbrains.annotations.Nullable;

/// `CommonPlayerSpawnInfo`, shared by `login` and `respawn`. [#dimensionTypeId()] indexes the
/// `dimension_type` registry sent during configuration, which is where the world model gets its
/// min y and height.
public sealed interface CommonPlayerSpawnInfo permits CommonPlayerSpawnInfo.V776, CommonPlayerSpawnInfo.V777 {

    int dimensionTypeId();

    String dimension();

    int gameType();

    @Nullable GlobalPos lastDeathLocation();

    void encode(ByteWriter writer);

    record V776(
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
    ) implements CommonPlayerSpawnInfo {

        public static V776 decode(ByteReader reader) {
            return new V776(
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

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(dimensionTypeId).utf(dimension).i64(seed)
                .u8(gameType).u8(previousGameType)
                .bool(isDebug).bool(isFlat)
                .optional(lastDeathLocation, (out, pos) -> pos.encode(out))
                .varInt(portalCooldown).varInt(seaLevel);
        }
    }

    /// Both game modes became varints, the previous one through `OPTIONAL_VAR_INT` (id + 1, zero
    /// for none) — which is -1 for none here, as the byte was in 26.2.
    record V777(
        int dimensionTypeId,
        String dimension,
        long seed,
        int gameType,
        int previousGameType,
        boolean isDebug,
        boolean isFlat,
        @Nullable GlobalPos lastDeathLocation,
        int portalCooldown,
        int seaLevel
    ) implements CommonPlayerSpawnInfo {

        public static V777 decode(ByteReader reader) {
            return new V777(
                reader.varInt(),
                reader.utf(),
                reader.i64(),
                reader.varInt(),
                reader.varInt() - 1,
                reader.bool(),
                reader.bool(),
                reader.optional(GlobalPos::decode),
                reader.varInt(),
                reader.varInt());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(dimensionTypeId).utf(dimension).i64(seed)
                .varInt(gameType).varInt(previousGameType + 1)
                .bool(isDebug).bool(isFlat)
                .optional(lastDeathLocation, (out, pos) -> pos.encode(out))
                .varInt(portalCooldown).varInt(seaLevel);
        }
    }

    /// A dimension plus a `BlockPos#asLong`.
    record GlobalPos(String dimension, long packedPos) {

        public static GlobalPos decode(ByteReader reader) {
            return new GlobalPos(reader.utf(), reader.blockPos());
        }

        public void encode(ByteWriter writer) {
            writer.utf(dimension).blockPos(packedPos);
        }
    }
}
