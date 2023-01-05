package net.hollowcube.common.result;

import net.hollowcube.util.FutureUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

class FutureResults {

    /**
     * {@link CompletableFuture} backed {@link FutureResult}.
     */
    public static final class CF<T> implements FutureResult<T> {
        private final CompletableFuture<Result<T>> future;

        public CF(@NotNull CompletableFuture<Result<T>> future) {
            this.future = future;
        }

        @Override
        public @NotNull FutureResult<Void> then(@NotNull Consumer<T> consumer) {
            return new CF<>(future.thenApply(result -> {
                try {
                    if (result.isErr()) return Result.error(result.error());
                    consumer.accept(result.result());
                    return Result.ofNull();
                } catch (Throwable t) {
                    return Result.error(Error.of(t));
                }
            }));
        }

        @Override
        public @NotNull FutureResult<Void> thenErr(@NotNull Consumer<Error> consumer) {
            return new CF<>(future.thenApply(result -> {
                try {
                    if (result.isErr()) consumer.accept(result.error());
                    return Result.ofNull();
                } catch (Throwable t) {
                    // This is typically a terminating call, so we do the equivalent of rethrowing the exception.
                    FutureUtil.handleException(t);
                    return Result.error(Error.of(t));
                }
            }));
        }

        @Override
        public @NotNull <S> FutureResult<S> map(@NotNull Function<T, S> mapper) {
            return new CF<>(future.thenApply(result -> {
                try {
                    if (result.isErr())
                        return Result.error(result.error());
                    return Result.of(mapper.apply(result.result()));
                } catch (Throwable t) {
                    return Result.error(Error.of(t));
                }
            }));
        }

        @Override
        public @NotNull FutureResult<T> mapErr(@NotNull Function<@NotNull Error, @NotNull Result<T>> mapper) {
            return new CF<>(future.thenApply(result -> {
                try {
                    if (result.isErr())
                        return mapper.apply(result.error());
                    return result;
                } catch (Throwable t) {
                    return Result.error(Error.of(t));
                }
            }));
        }

        @Override
        public @NotNull <S> FutureResult<S> flatMap(@NotNull Function<T, @NotNull FutureResult<S>> mapper) {
            return new CF<>(future.thenCompose(result -> {
                try {
                    if (result.isErr())
                        return CompletableFuture.completedFuture(Result.error(result.error()));
                    return mapper.apply(result.result()).toCompletableFuture();
                } catch (Throwable t) {
                    return FutureResult.<S>error(Error.of(t)).toCompletableFuture();
                }
            }));
        }

        @Override
        public @NotNull FutureResult<T> flatMapErr(@NotNull Function<@NotNull Error, @NotNull FutureResult<T>> mapper) {
            return new CF<>(future.thenCompose(result -> {
                try {
                if (result.isErr())
                    return mapper.apply(result.error()).toCompletableFuture();
                return CompletableFuture.completedFuture(result);
                } catch (Throwable t) {
                    return FutureResult.<T>error(Error.of(t)).toCompletableFuture();
                }
            }));
        }

        @Override
        public @NotNull FutureResult<T> wrapErr(@NotNull String format) {
            return new CF<>(future.thenApply(result -> {
                try {
                    if (result.isErr())
                        return Result.error(result.error().wrap(format));
                    return result;
                } catch (Throwable t) {
                    return Result.error(Error.of(t));
                }
            }));
        }

        @Override
        public @NotNull FutureResult<T> alsoRaw(@NotNull Consumer<@NotNull Result<T>> consumer) {
            return new CF<>(future.thenApply(result -> {
                try {
                    consumer.accept(result);
                    return result;
                } catch (Throwable t) {
                    return Result.error(Error.of(t));
                }
            }));
        }

        @Override
        public @NotNull <S> FutureResult<T> flatAlso(@NotNull Function<T, @NotNull FutureResult<S>> mapper) {
            return new CF<>(future.thenCompose(result -> {
                try {
                    if (result.isErr())
                        return CompletableFuture.completedFuture(result);
                    return mapper.apply(result.result()).toCompletableFuture().thenApply(r -> result);
                } catch (Throwable t) {
                    return FutureResult.<T>error(Error.of(t)).toCompletableFuture();
                }
            }));
        }

        @Override
        public @NotNull Result<T> await() {
            return future.join();
        }

        @Override
        public @NotNull CompletableFuture<Result<T>> toCompletableFuture() {
            return future;
        }
    }

}
