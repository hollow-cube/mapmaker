package net.hollowcube.common.config;

import org.jetbrains.annotations.NotNull;

public interface ConfigProvider {

    <C extends Record> @NotNull C get(@NotNull Class<C> clazz);

}
