package net.hollowcube.mapmaker.runtime.parkour.replay.event;

import dev.hollowcube.replay.event.ReplayEvent;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record RunStartEvent() implements ReplayEvent {
    public static final NetworkBuffer.Type<RunStartEvent> NETWORK_TYPE =
        NetworkBufferTemplate.template(RunStartEvent::new);
}
