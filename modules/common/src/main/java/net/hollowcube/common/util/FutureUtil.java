package net.hollowcube.common.util;

import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class FutureUtil {
    private FutureUtil() {
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T> @Nullable T handleException(@NotNull Throwable t) {
        MinecraftServer.getExceptionManager().handleException(t);
        return null;
    }

    public static <T> @NotNull Consumer<T> virtual(@NotNull Consumer<T> consumer) {
        return value -> Thread.startVirtualThread(() -> consumer.accept(value));
    }

    public static @NotNull Callable<Void> call(@NotNull Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
            return null;
        };
    }

}
