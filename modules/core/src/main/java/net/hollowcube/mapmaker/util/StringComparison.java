package net.hollowcube.mapmaker.util;

import info.debatty.java.stringsimilarity.JaroWinkler;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.function.Function;

public final class StringComparison {
    private static final JaroWinkler JARO_WINKLER = new JaroWinkler();

    public static <T> @NotNull Comparator<T> jaroWinkler(@NotNull String query, @NotNull Function<T, String> keyFunc) {
        return Comparator.comparingDouble((T t) -> JARO_WINKLER.similarity(query, keyFunc.apply(t))).reversed();
    }

}
