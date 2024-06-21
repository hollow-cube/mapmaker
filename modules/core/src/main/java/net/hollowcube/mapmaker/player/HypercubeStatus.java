package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public record HypercubeStatus(int exp, @NotNull Instant since, @NotNull Instant until) {

}
