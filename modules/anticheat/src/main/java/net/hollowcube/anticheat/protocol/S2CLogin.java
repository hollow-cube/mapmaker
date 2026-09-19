package net.hollowcube.anticheat.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/// `play login`: the own entity id and the dimension the world model is built against.
public sealed interface S2CLogin extends Packet permits S2CLogin.V776, S2CLogin.V777 {

    int playerId();

    CommonPlayerSpawnInfo spawnInfo();

    int chunkRadius();

    int simulationDistance();

    record V776(
        int playerId,
        boolean hardcore,
        List<String> levels,
        int maxPlayers,
        int chunkRadius,
        int simulationDistance,
        boolean reducedDebugInfo,
        boolean showDeathScreen,
        boolean doLimitedCrafting,
        CommonPlayerSpawnInfo.V776 spawnInfo,
        boolean onlineMode,
        boolean enforcesSecureChat
    ) implements S2CLogin {

        public static V776 decode(ByteReader reader) {
            return decode(reader, CommonPlayerSpawnInfo.V776::decode, V776::new);
        }

        static <S extends CommonPlayerSpawnInfo, L extends S2CLogin> L decode(
            ByteReader reader, Function<ByteReader, S> spawnInfo, Layout<S, L> layout
        ) {
            int playerId = reader.i32();
            boolean hardcore = reader.bool();
            int levelCount = reader.varInt();
            if (levelCount < 0 || levelCount > reader.remaining())
                throw new ProtocolException("bad level count: " + levelCount);
            var levels = new ArrayList<String>(levelCount);
            for (int i = 0; i < levelCount; i++) levels.add(reader.utf());
            return layout.create(playerId, hardcore, List.copyOf(levels),
                reader.varInt(), reader.varInt(), reader.varInt(),
                reader.bool(), reader.bool(), reader.bool(),
                spawnInfo.apply(reader), reader.bool(), reader.bool());
        }

        @FunctionalInterface
        interface Layout<S extends CommonPlayerSpawnInfo, L extends S2CLogin> {
            L create(
                int playerId, boolean hardcore, List<String> levels, int maxPlayers, int chunkRadius,
                int simulationDistance, boolean reducedDebugInfo, boolean showDeathScreen,
                boolean doLimitedCrafting, S spawnInfo, boolean onlineMode, boolean enforcesSecureChat);
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.i32(playerId).bool(hardcore).varInt(levels.size());
            for (var level : levels) writer.utf(level);
            writer.varInt(maxPlayers).varInt(chunkRadius).varInt(simulationDistance)
                .bool(reducedDebugInfo).bool(showDeathScreen).bool(doLimitedCrafting);
            spawnInfo.encode(writer);
            writer.bool(onlineMode).bool(enforcesSecureChat);
        }
    }

    /// 26.2's layout around [CommonPlayerSpawnInfo.V777].
    record V777(
        int playerId,
        boolean hardcore,
        List<String> levels,
        int maxPlayers,
        int chunkRadius,
        int simulationDistance,
        boolean reducedDebugInfo,
        boolean showDeathScreen,
        boolean doLimitedCrafting,
        CommonPlayerSpawnInfo.V777 spawnInfo,
        boolean onlineMode,
        boolean enforcesSecureChat
    ) implements S2CLogin {

        public static V777 decode(ByteReader reader) {
            return V776.decode(reader, CommonPlayerSpawnInfo.V777::decode, V777::new);
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.i32(playerId).bool(hardcore).varInt(levels.size());
            for (var level : levels) writer.utf(level);
            writer.varInt(maxPlayers).varInt(chunkRadius).varInt(simulationDistance)
                .bool(reducedDebugInfo).bool(showDeathScreen).bool(doLimitedCrafting);
            spawnInfo.encode(writer);
            writer.bool(onlineMode).bool(enforcesSecureChat);
        }
    }
}
