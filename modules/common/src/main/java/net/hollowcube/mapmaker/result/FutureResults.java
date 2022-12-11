package net.hollowcube.mapmaker.result;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;

class FutureResults {

    /** {@link CompletableFuture} backed {@link FutureResult}. */
    public static final class CF<T> implements FutureResult<T> {
        private final CompletableFuture<Result<T>> future;

        public CF(@NotNull CompletableFuture<Result<T>> future) {
            this.future = future;
        }

        @Override
        public @NotNull FutureResult<Void> then(@NotNull Consumer<T> consumer) {
            return new CF<>(future.thenApply(result -> {
                if (result.isErr()) return Result.error(result.error());
                consumer.accept(result.result());
                return Result.ofNull();
            }));
        }

        @Override
        public @NotNull <S> FutureResult<S> map(@NotNull Function<T, S> mapper) {
            return new CF<>(future.thenApply(result -> {
                if (result.isErr())
                    return Result.error(result.error());
                return Result.of(mapper.apply(result.result()));
            }));
        }

        @Override
        public @NotNull FutureResult<T> mapErr(@NotNull Function<@NotNull Error, @NotNull Result<T>> mapper) {
            return new CF<>(future.thenApply(result -> {
                if (result.isErr())
                    return mapper.apply(result.error());
                return result;
            }));
        }

        @Override
        public @NotNull <S> FutureResult<S> flatMap(@NotNull Function<T, @NotNull FutureResult<S>> mapper) {
            return new CF<>(future.thenCompose(result -> {
                if (result.isErr())
                    return CompletableFuture.completedFuture(Result.error(result.error()));
                return mapper.apply(result.result()).toCompletableFuture();
            }));
        }

        @Override
        public @NotNull FutureResult<T> flatMapErr(@NotNull Function<@NotNull Error, @NotNull FutureResult<T>> mapper) {
            return new CF<>(future.thenCompose(result -> {
                if (result.isErr())
                    return mapper.apply(result.error()).toCompletableFuture();
                return CompletableFuture.completedFuture(result);
            }));
        }

        @Override
        public @NotNull CompletableFuture<Result<T>> toCompletableFuture() {
            return future;
        }
    }

}
