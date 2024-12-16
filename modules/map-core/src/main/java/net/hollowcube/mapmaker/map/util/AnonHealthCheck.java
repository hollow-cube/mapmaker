package net.hollowcube.mapmaker.map.util;

import net.hollowcube.mapmaker.util.HttpServerWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

public record AnonHealthCheck(
        @NotNull String name,
        @NotNull BooleanSupplier fn
) implements HttpServerWrapper.HealthCheck {

    @Override
    public boolean healthCheck() {
        return fn.getAsBoolean();
    }
}
