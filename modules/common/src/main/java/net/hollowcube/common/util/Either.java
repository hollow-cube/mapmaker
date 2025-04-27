package net.hollowcube.common.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public sealed interface Either<L, R> {
    static <L, R> Either<L, R> left(L left) {
        return new Either.Left<>(left);
    }

    static <L, R> Either<L, R> right(R right) {
        return new Either.Right<>(right);
    }

    default boolean isLeft() {
        return false;
    }

    default boolean isRight() {
        return false;
    }

    default L left() {
        throw new IllegalStateException("Either is not left");
    }

    default R right() {
        throw new IllegalStateException("Either is not right");
    }

    default L leftOr(L defaultValue) {
        return defaultValue;
    }

    default R rightOr(R defaultValue) {
        return defaultValue;
    }

    <T> T map(@NotNull Function<L, T> leftMapper, @NotNull Function<R, T> rightMapper);

    record Left<L, R>(@Nullable L left) implements Either<L, R> {
        @Override
        public boolean isLeft() {
            return true;
        }

        @Override
        public L leftOr(L defaultValue) {
            return left;
        }

        @Override
        public <T> T map(@NotNull Function<L, T> leftMapper, @NotNull Function<R, T> rightMapper) {
            return leftMapper.apply(left);
        }
    }

    record Right<L, R>(@Nullable R right) implements Either<L, R> {
        @Override
        public boolean isRight() {
            return true;
        }

        @Override
        public R rightOr(R defaultValue) {
            return right;
        }

        @Override
        public <T> T map(@NotNull Function<L, T> leftMapper, @NotNull Function<R, T> rightMapper) {
            return rightMapper.apply(right);
        }
    }

}
