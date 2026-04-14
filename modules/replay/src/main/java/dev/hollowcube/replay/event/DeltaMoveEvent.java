package dev.hollowcube.replay.event;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;

public record DeltaMoveEvent(int entityId, Pos delta) implements ReplayEvent {

    @Override
    public void write(NetworkBuffer buffer) {
        buffer.write(NetworkBuffer.BYTE, (byte) 0x01); // TODO: id, obvs shouldnt be written here
        buffer.write(NetworkBuffer.VAR_INT, entityId);
        buffer.write(NetworkBuffer.POS, delta);
    }

}
