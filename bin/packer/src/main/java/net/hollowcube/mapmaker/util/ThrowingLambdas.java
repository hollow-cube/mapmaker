package net.hollowcube.mapmaker.util;

public final class ThrowingLambdas {

    @FunctionalInterface
    public interface BiConsumer<A, B> {
        void accept(A a, B b) throws Exception;
    }
}
