package net.hollowcube.canvas.internal;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.ServiceLoader;

final class Globals {
    private Globals() {}

    private static ControllerFactory factory = null;

    public static @NotNull ControllerFactory factory() {
        if (factory == null) {
            factory = ServiceLoader.load(ControllerFactory.class)
                    .findFirst().orElseThrow();
        }
        return factory;
    }

    private static Controller controller = null;

    public static @NotNull Controller controller() {
        if (controller == null) {
            controller = factory().create(Map.of());
        }
        return controller;
    }

    public static void replace(@NotNull Controller controller) {
        Globals.controller = controller;
    }

}
