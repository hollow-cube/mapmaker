package net.hollowcube.mapmaker.config;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record HttpConfig(String host, int port) {
}
