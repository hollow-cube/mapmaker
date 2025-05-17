package net.hollowcube.mapmaker.player;

import com.google.gson.annotations.SerializedName;
import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record JoinMapResponse(
        @NotNull String server,
        @SerializedName("serverClusterIP")
        @NotNull String serverClusterIp
) {
}
