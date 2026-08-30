package net.hollowcube.anticheat.protocol;

/// The connection's protocol state, which selects the packet id table.
///
/// Handshake and login are present so the registry can answer for them; the tap is installed at
/// `PostLoginEvent` and never sees their frames.
public enum ProtocolState {
    HANDSHAKE,
    LOGIN,
    CONFIGURATION,
    PLAY
}
