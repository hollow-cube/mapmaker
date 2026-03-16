package net.hollowcube.mapmaker.player;

import com.google.gson.annotations.SerializedName;
import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record JoinMapResponse(
    String server,

    @SerializedName("serverClusterIP")
    String serverClusterIp
) {
}
