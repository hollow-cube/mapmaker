package net.hollowcube.anticheat.protocol;

/// A packet whose state-cache key is a container id.
public non-sealed interface ContainerKeyed extends Packet {
    int containerId();
}
