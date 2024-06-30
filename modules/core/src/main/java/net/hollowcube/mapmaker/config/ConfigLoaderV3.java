package net.hollowcube.mapmaker.config;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface ConfigLoaderV3 {
    static @NotNull ConfigLoaderV3 loadDefault(String[] args) {
        return ConfigLoaderV3Impl.loadDefault(args);
    }

    static @NotNull ConfigLoaderV3 loadFromText(byte @NotNull [] text, @NotNull Map<String, String> env) {
        return ConfigLoaderV3Impl.loadFromText(text, env);
    }

    <C> @NotNull C get(@NotNull Class<C> clazz);
}
