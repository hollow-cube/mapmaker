package net.hollowcube.mapmaker.runtime.parkour.replay.event;

import dev.hollowcube.replay.event.ReplayEvent;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

/// Puts every ghost block a replay has set back the way the world had it.
///
/// A snapshot describes the ghost blocks that exist at that moment, and cannot describe the ones
/// that do not, so it opens with this rather than leaving whatever an earlier playback position set.
public record ClearGhostBlocksEvent() implements ReplayEvent {
    public static final NetworkBuffer.Type<ClearGhostBlocksEvent> NETWORK_TYPE =
        NetworkBufferTemplate.template(ClearGhostBlocksEvent::new);
}
