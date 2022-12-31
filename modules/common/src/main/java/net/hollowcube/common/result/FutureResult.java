package net.hollowcube.common.result;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface FutureResult<T> permits FutureResults.CF {

    // Factory methods

    /** Creates a new {@link FutureResult} which is already completed with the given result. */
    static <T> @NotNull FutureResult<T> of(T result) {
        return new FutureResults.CF<>(CompletableFuture.completedFuture(Result.of(result)));
    }

    /** Creates a new {@link FutureResult} which is already completed with the given {@link Error}. */
    static <T> @NotNull FutureResult<T> error(@NotNull Error error) {
        return new FutureResults.CF<>(CompletableFuture.completedFuture(Result.error(error)));
    }

    /** Creates a new {@link FutureResult} which is already completed with a null result and no error. */
    static <T> @NotNull FutureResult<T> ofNull() {
        return new FutureResults.CF<>(CompletableFuture.completedFuture(Result.ofNull()));
    }

    /** Creates a new {@link FutureResult} executing the given {@link Supplier} in the jvm common pool. */
    static <T> @NotNull FutureResult<T> supply(@NotNull Supplier<@NotNull Result<T>> supplier) {
        return new FutureResults.CF<>(CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Throwable t) {
                return Result.error(Error.of(t));
            }
        }, ForkJoinPool.commonPool()));
    }

    /**
     * Creates a new {@link FutureResult} which completes when all the given {@link FutureResult}s are complete.
     *
     * @apiNote The returned {@link FutureResult} will fail with all errors when all futures are complete.
     * todo maybe should just return first error
     */
    static @NotNull FutureResult<Void> allOf(@NotNull FutureResult<?>... results) {
        var futures = new CompletableFuture[results.length];
        for (int i = 0; i < results.length; i++)
            futures[i] = results[i].toCompletableFuture();
        return new FutureResults.CF<>(CompletableFuture.allOf(futures).thenApply(v -> Result.of(null)));
    }


    // Handler methods

    @NotNull FutureResult<Void> then(@NotNull Consumer<T> consumer);
    @NotNull FutureResult<Void> thenErr(@NotNull Consumer<Error> consumer);

    @NotNull <S> FutureResult<S> map(@NotNull Function<T, S> mapper);
    @NotNull FutureResult<T> mapErr(@NotNull Function<@NotNull Error, @NotNull Result<T>> mapper);

    @NotNull <S> FutureResult<S> flatMap(@NotNull Function<T, @NotNull FutureResult<S>> mapper);
    @NotNull FutureResult<T> flatMapErr(@NotNull Function<@NotNull Error, @NotNull FutureResult<T>> mapper);


    // Compatibility

    static <T> @NotNull FutureResult<T> wrap(@NotNull CompletableFuture<T> future) {
        //todo need to wrap exceptions in Error type here
        return new FutureResults.CF<>(future
                .thenApply(Result::of)
                .exceptionally(e -> Result.error(Error.of(e))));
    }

    @NotNull CompletableFuture<Result<T>> toCompletableFuture();

}
