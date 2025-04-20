package net.hollowcube.common.util;

public class WordUtil {
    public static String indefiniteArticle(final String word) {
        // building -> a building
        // adventure -> an adventure
        return switch (word.charAt(0)) {
            case 'a', 'e', 'i', 'o', 'u' -> "an";
            default -> "a";
        } + " " + word;
    }
}
