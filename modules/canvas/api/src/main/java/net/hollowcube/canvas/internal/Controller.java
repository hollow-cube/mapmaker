package net.hollowcube.canvas.internal;

import net.hollowcube.canvas.View;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public interface Controller {

    static @NotNull Controller make() {
        return Globals.factory().create(Map.of());
    }

    static @NotNull Controller make(@NotNull Map<String, Object> context) {
        return Globals.factory().create(context);
    }

    static void replaceGlobals(@NotNull Controller controller) {
        Globals.replace(controller);
    }

    void show(@NotNull Player player, @NotNull Function<Context, View> viewProvider);

}
