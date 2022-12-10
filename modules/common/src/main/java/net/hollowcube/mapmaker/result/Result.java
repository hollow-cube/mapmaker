package net.hollowcube.mapmaker.result;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public record Result<T>(@UnknownNullability T result, @UnknownNullability Error error) {

    public static <T> @NotNull Result<T> of(@UnknownNullability T result) {
        return new Result<>(result, null);
    }
    public static <T> @NotNull Result<T> error(@NotNull Error err) {
        return new Result<>(null, err);
    }

    public boolean isErr() {
        return error != null;
    }

}
