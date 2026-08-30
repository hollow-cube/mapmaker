package net.hollowcube.anticheat.protocol;

/// A packet whose state-cache key is an entity id, whether or not the rest of it is decoded.
public non-sealed interface EntityKeyed extends Packet {
    int entityId();
}
