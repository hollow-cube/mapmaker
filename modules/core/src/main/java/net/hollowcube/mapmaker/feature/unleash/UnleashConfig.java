package net.hollowcube.mapmaker.feature.unleash;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record UnleashConfig(
        boolean enabled,
        @NotNull String address,
        @NotNull String token
) {
}
