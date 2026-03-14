package net.hollowcube.common.util;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public sealed interface Either<L, R> {
    static <L, R> Either<L, R> left(@Nullable L left) {
        return new Either.Left<>(left);
    }

    static <L, R> Either<L, R> right(@Nullable R right) {
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

    default @Nullable L leftOr(L defaultValue) {
        return defaultValue;
    }

    default @Nullable R rightOr(R defaultValue) {
        return defaultValue;
    }

    <T> T map(Function<@Nullable L, T> leftMapper, Function<@Nullable R, T> rightMapper);

    void consume(Consumer<@Nullable L> leftConsumer, Consumer<@Nullable R> rightConsumer);

    record Left<L, R>(@Nullable L left) implements Either<L, R> {
        @Override
        public boolean isLeft() {
            return true;
        }

        @Override
        public @Nullable L leftOr(L defaultValue) {
            return left;
        }

        @Override
        public <T> T map(Function<@Nullable L, T> leftMapper, Function<@Nullable R, T> rightMapper) {
            return leftMapper.apply(left);
        }

        @Override
        public void consume(Consumer<@Nullable L> leftConsumer, Consumer<@Nullable R> rightConsumer) {
            leftConsumer.accept(left);
        }
    }

    record Right<L, R>(@Nullable R right) implements Either<L, R> {
        @Override
        public boolean isRight() {
            return true;
        }

        @Override
        public @Nullable R rightOr(R defaultValue) {
            return right;
        }

        @Override
        public <T> T map(Function<@Nullable L, T> leftMapper, Function<@Nullable R, T> rightMapper) {
            return rightMapper.apply(right);
        }

        @Override
        public void consume(Consumer<@Nullable L> leftConsumer, Consumer<@Nullable R> rightConsumer) {
            rightConsumer.accept(right);
        }
    }

}
