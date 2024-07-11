package net.hollowcube.mapmaker.map.util;

import io.helidon.health.HealthCheck;
import io.helidon.health.HealthCheckResponse;
import io.helidon.health.HealthCheckType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record AnonHealthCheck(@NotNull String name, @NotNull HealthCheckType type,
                              @NotNull Supplier<HealthCheckResponse> fn) implements HealthCheck {

    public AnonHealthCheck(@NotNull String name, @NotNull Supplier<Boolean> fn) {
        this(name, HealthCheckType.READINESS, () -> HealthCheckResponse.builder().status(fn.get()).build());
    }

    @Override
    public HealthCheckResponse call() {
        return fn.get();
    }
}
