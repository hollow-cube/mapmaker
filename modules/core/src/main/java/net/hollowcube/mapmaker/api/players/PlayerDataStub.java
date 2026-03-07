package net.hollowcube.mapmaker.api.players;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.player.DisplayName;

@RuntimeGson
public record PlayerDataStub(String id, DisplayName displayName) {
}
