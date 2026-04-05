package net.hollowcube.mapmaker.api.players;

import net.hollowcube.common.util.RuntimeGson;

import java.time.Instant;

@RuntimeGson
public record Hypercube(int exp, Instant start, Instant end) {

}
