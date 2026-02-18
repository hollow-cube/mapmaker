package net.hollowcube.mapmaker.util.nats;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record NatsConfig(String servers) {
}
