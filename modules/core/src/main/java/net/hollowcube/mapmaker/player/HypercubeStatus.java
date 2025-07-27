package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@RuntimeGson
public record HypercubeStatus(int exp, @NotNull Instant since, @NotNull Instant until) {

}
