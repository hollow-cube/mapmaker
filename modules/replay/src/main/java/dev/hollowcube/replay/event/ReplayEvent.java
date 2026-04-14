package dev.hollowcube.replay.event;

import net.minestom.server.network.NetworkBuffer;

public interface ReplayEvent {
    void write(NetworkBuffer buffer); // todo use separate registry of types probably
}
