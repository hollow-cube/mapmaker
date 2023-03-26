package net.hollowcube.canvas.internal;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface ControllerFactory {

    @NotNull Controller create(@NotNull Map<String, Object> context);

}
