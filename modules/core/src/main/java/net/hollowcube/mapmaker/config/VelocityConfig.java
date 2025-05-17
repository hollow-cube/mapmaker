package net.hollowcube.mapmaker.config;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record VelocityConfig(@NotNull String secret) {

}
