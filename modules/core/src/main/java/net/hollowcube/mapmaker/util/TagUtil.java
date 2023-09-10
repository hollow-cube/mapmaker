package net.hollowcube.mapmaker.util;

public final class TagUtil {

    public static <T, S> S noop(T t) {
        throw new IllegalStateException("Noop tag writer called");
    }

    private TagUtil() {}
}
