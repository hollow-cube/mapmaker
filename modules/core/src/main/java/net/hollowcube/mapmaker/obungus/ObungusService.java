package net.hollowcube.mapmaker.obungus;

import org.jetbrains.annotations.Blocking;

@Blocking
public interface ObungusService {

    ObungusBoxData getBoxFromReviewQueue(String playerId);

}
