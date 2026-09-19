package net.hollowcube.ipc.session;

/// A server a player can be sent to, as the proxy connects to it.
///
/// @param id              the pod name
/// @param protocolVersion what the server speaks, or 0 when the tracker has not learned it yet
public record GameServer(String id, String clusterIp, int protocolVersion) {}
