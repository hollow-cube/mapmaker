package net.hollowcube.mapmaker.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record MinestomConfig(String host, int port) {
}
