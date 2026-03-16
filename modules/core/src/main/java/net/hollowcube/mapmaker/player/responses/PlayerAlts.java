package net.hollowcube.mapmaker.player.responses;

import net.hollowcube.common.util.RuntimeGson;

import java.util.List;

@RuntimeGson
public record PlayerAlts(
    List<Alt> results
) {

    @RuntimeGson
    public record Alt(
        String username,
        String id
    ) {
    }
}
