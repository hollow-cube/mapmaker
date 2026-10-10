package dev.hollowcube.replay.event;

import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import net.minestom.server.entity.EntityType;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class LegacyIdsTest {
    private static final ChunkIndex LEGACY = new ChunkIndex(0, 1, (byte) 0, 0, 1, 1,
        ReplayHeader.VERSION_LEGACY_IDS, ReplayHeader.LEGACY_IDS_DATA_VERSION);

    /// The size of the 26.2 registries.
    private static final int BLOCK_STATES = 32366;
    private static final int ENTITY_TYPES = 158;

    @Test
    void everyLegacyBlockStateResolvesToday() {
        for (var id = 0; id < BLOCK_STATES; id++) {
            var buffer = NetworkBuffer.resizableBuffer();
            buffer.write(NetworkBuffer.VAR_INT, id);
            var block = ReplayGameData.readBlock(buffer, LEGACY);
            var state = LegacyIds.blockState(id);
            var properties = state.indexOf('[');
            assertEquals(properties == -1 ? state : state.substring(0, properties), block.name(), state);
            assertEquals(properties == -1 ? Map.of() : parseProperties(state.substring(properties)), block.properties(), state);
        }
        assertThrows(IllegalStateException.class, () -> LegacyIds.blockState(BLOCK_STATES));
    }

    private static Map<String, String> parseProperties(String properties) {
        var parsed = new HashMap<String, String>();
        for (var property : properties.substring(1, properties.length() - 1).split(",")) {
            var separator = property.indexOf('=');
            parsed.put(property.substring(0, separator), property.substring(separator + 1));
        }
        return parsed;
    }

    @Test
    void everyLegacyEntityTypeResolvesToday() {
        for (var id = 0; id < ENTITY_TYPES; id++) {
            var buffer = NetworkBuffer.resizableBuffer();
            buffer.write(NetworkBuffer.VAR_INT, id);
            var entityType = ReplayGameData.readEntityType(buffer, LEGACY);
            assertEquals(LegacyIds.entityType(id), entityType.key().asString(), "26.2 entity type " + id);
        }
        assertThrows(IllegalStateException.class, () -> LegacyIds.entityType(ENTITY_TYPES));
    }

    @Test
    void theMappingMatchesKnown26_2Ids() {
        assertEquals("minecraft:air", LegacyIds.blockState(0));
        assertEquals("minecraft:oak_stairs[facing=north,half=top,shape=straight,waterlogged=true]",
            LegacyIds.blockState(3907));
        assertEquals("minecraft:stray", LegacyIds.entityType(128));
        assertEquals(EntityType.ARMOR_STAND.key().asString(), LegacyIds.entityType(5));
    }
}
