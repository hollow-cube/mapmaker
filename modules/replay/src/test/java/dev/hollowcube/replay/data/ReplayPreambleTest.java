package dev.hollowcube.replay.data;

import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class ReplayPreambleTest {

    @Test
    void aBadLengthSaysWhichFieldItCameFrom() {
        var header = new ReplayHeader(UUID.randomUUID(), ReplayHeader.worldVersion(UUID.randomUUID()));
        header.update(-1, 0, 0, 0);

        var data = new byte[ReplayHeader.HEADER_LENGTH];
        header.write(NetworkBuffer.wrap(data, 0, 0));

        var error = assertThrows(IllegalArgumentException.class, () -> ReplayPreamble.read(data));
        assertTrue(error.getMessage().contains("metadata=-1"), error.getMessage());
    }

    @Test
    void aTruncatedPreambleSaysHowShortItIs() {
        var error = assertThrows(IllegalArgumentException.class, () -> ReplayPreamble.read(new byte[7]));
        assertTrue(error.getMessage().contains("got 7"), error.getMessage());
    }
}
