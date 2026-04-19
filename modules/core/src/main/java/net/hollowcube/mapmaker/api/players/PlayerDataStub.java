package net.hollowcube.mapmaker.api.players;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerSetting;

@RuntimeGson
public record PlayerDataStub(String id, DisplayName displayName, JsonObject settings) {

    public <T> T getSetting(PlayerSetting<T> setting) {
        return setting.read(settings());
    }

}
