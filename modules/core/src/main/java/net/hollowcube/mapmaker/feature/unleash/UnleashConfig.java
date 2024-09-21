package net.hollowcube.mapmaker.feature.unleash;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record UnleashConfig(
        boolean enabled,
        boolean usePosthog,
        @NotNull String address,
        @NotNull String token,
        @Setting("default_action")
        boolean defaultAction
) {
}
