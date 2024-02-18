package net.hollowcube.mapmaker.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record GlobalConfig(
        boolean noop
) {
}
