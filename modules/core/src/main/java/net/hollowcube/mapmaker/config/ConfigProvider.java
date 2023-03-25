package net.hollowcube.mapmaker.config;

import org.jetbrains.annotations.NotNull;

public interface ConfigProvider {

    <C extends Record> @NotNull C get(@NotNull Class<C> clazz);

}
