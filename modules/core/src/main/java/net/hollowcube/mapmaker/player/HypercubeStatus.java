package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;

import java.time.Instant;

@RuntimeGson
public record HypercubeStatus(int exp, Instant since, Instant until) {

}
