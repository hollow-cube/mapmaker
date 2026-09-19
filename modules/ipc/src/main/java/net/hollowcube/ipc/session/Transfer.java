package net.hollowcube.ipc.session;

import net.hollowcube.ipc.util.Payload;

/// Sent by a game server to have the proxy move its player to another server.
///
/// @param server          the target's `server_states` id
/// @param protocolVersion what the target speaks, or 0 when the tracker has not learned it yet
@Payload(Transfer.CHANNEL)
public record Transfer(String server, String address, int protocolVersion) {
    public static final String CHANNEL = "mapmaker:transfer";
}
