package net.hollowcube.datafix.blockEntity;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Transcoder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class V99_03_Control extends AbstractBlockEntityUpgradeTest {
    private static final int CURRENT = 4314;

    @Test
    void upgradeId() {
        var result = upgradeC(0, CURRENT);
        assertEquals("minecraft:command_block", result.getString("id"));
    }

    @Test
    void expectedKeys() {
        var result = upgradeC(0, CURRENT);
        assertEquals(Set.of("x", "y", "z", "id", "Command", "CustomName", "SuccessCount", "LastOutput", "TrackOutput"), result.keySet());
    }

    @Test
    void validTextComponents() {
        var result = upgradeC(0, CURRENT);

        assertDoesNotThrow(() -> Codec.COMPONENT.decode(Transcoder.NBT, result.get("CustomName")).orElseThrow());
        assertDoesNotThrow(() -> Codec.COMPONENT.decode(Transcoder.NBT, result.get("LastOutput")).orElseThrow());
    }

}
