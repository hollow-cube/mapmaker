package net.hollowcube.common.util;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public class Either<L, R> {
    public static <L, R> Either<L, R> left(L left) {
        return new Either<>(left, null);
    }

    public static <L, R> Either<L, R> right(R right) {
        return new Either<>(null, right);
    }

    private final L left;
    private final R right;

    private Either(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public <T> T map(@NotNull Function<L, T> leftMapper, @NotNull Function<R, T> rightMapper) {
        if (left != null) {
            return leftMapper.apply(left);
        } else if (right != null) {
            return rightMapper.apply(right);
        } else {
            throw new IllegalStateException("Either is empty");
        }
    }

    public boolean isLeft() {
        return left != null;
    }

    public boolean isRight() {
        return right != null;
    }

    public L left() {
        return Objects.requireNonNull(left, "Either is not left");
    }

    public R right() {
        return Objects.requireNonNull(right, "Either is not right");
    }

    public L leftOr(L defaultValue) {
        return left != null ? left : defaultValue;
    }

    public R rightOr(R defaultValue) {
        return right != null ? right : defaultValue;
    }

}
