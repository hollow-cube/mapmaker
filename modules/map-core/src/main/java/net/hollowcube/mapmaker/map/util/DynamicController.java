package net.hollowcube.mapmaker.map.util;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.Controller;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class DynamicController implements Controller {
    private final Map<String, Object> contextMap = new ConcurrentHashMap<>();
    private volatile boolean dirty = true;

    private volatile Controller controller = null;

    public void addBinding(@NotNull String key, @NotNull Object value) {
        contextMap.put(key, value);
        dirty = true;
    }

    @Override
    public void show(@NotNull Player player, @NotNull Function<Context, View> viewProvider) {
        if (dirty) {
            controller = Controller.make(contextMap);
            dirty = false;
        }

        Objects.requireNonNull(controller).show(player, viewProvider);
    }
}
