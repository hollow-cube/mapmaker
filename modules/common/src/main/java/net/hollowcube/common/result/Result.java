package net.hollowcube.common.result;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public record Result<T>(@UnknownNullability T result, @UnknownNullability Error error) {
    static final Result<Object> NULL = new Result<>(null, null);

    public static <T> @NotNull Result<T> of(@UnknownNullability T result) {
        return new Result<>(result, null);
    }
    public static <T> @NotNull Result<T> error(@NotNull Error err) {
        return new Result<>(null, err);
    }

    public static <T> @NotNull Result<T> ofNull() {
        //noinspection unchecked
        return (Result<T>) NULL;
    }

    public boolean isErr() {
        return error != null;
    }

}
