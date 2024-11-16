package net.hollowcube.mapmaker.obungus;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

@Blocking
public interface ObungusService {

    @NotNull ObungusBoxData getBoxFromReviewQueue(@NotNull String playerId);

}
