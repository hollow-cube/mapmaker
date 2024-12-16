package net.hollowcube.mapmaker.config;

import org.jetbrains.annotations.NotNull;

public record HttpConfig(@NotNull String host, int port) {
}
