package net.hollowcube.command.example.map.arg;

import net.hollowcube.command.arg.Argument;
import org.jetbrains.annotations.NotNull;

public final class ExtraArguments {

    public static @NotNull Argument<String> PlayerIdWithCompletion(@NotNull String id) {
        return null;
    }

    private ExtraArguments() {
    }
}
