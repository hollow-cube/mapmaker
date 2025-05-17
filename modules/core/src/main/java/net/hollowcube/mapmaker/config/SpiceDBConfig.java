package net.hollowcube.mapmaker.config;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record SpiceDBConfig(
        String url, String token
) {
}
