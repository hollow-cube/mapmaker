package net.hollowcube.mapmaker.config;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record VelocityConfig(@NotNull String secret) {

}
