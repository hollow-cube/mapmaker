package net.hollowcube.common.util;

import org.jetbrains.annotations.NotNull;

public final class StringUtil {

    public static @NotNull String snakeToPascal(@NotNull String snakeCase) {
        StringBuilder pascalCase = new StringBuilder();
        for (String part : snakeCase.split("_")) {
            if (!part.isEmpty()) {
                pascalCase.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    pascalCase.append(part.substring(1).toLowerCase());
                }
            }
        }
        return pascalCase.toString();
    }

    public static @NotNull String pascalToSnake(@NotNull String pascalCase) {
        StringBuilder snakeCase = new StringBuilder();
        for (int i = 0; i < pascalCase.length(); i++) {
            char c = pascalCase.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                snakeCase.append('_');
            }
            snakeCase.append(Character.toLowerCase(c));
        }
        return snakeCase.toString();
    }

}
