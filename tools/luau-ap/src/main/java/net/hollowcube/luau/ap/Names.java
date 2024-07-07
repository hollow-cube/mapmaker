package net.hollowcube.luau.ap;

import org.jetbrains.annotations.NotNull;

final class Names {

    public static @NotNull String toPropertyName(@NotNull String methodName) {
        if (methodName.startsWith("get")) methodName = methodName.substring(3);
        return Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);
    }

    public static @NotNull String toMethodName(@NotNull String methodName) {
        return Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);
    }
}
