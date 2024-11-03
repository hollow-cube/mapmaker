package net.hollowcube.posthog.flag;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record PropertyGroup(
        @NotNull String type,
        @NotNull List<Object> values
) {
}
