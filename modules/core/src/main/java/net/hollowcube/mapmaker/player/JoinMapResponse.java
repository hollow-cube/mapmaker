package net.hollowcube.mapmaker.player;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

public record JoinMapResponse(
        @NotNull String server,
        @SerializedName("serverClusterIP")
        @NotNull String serverClusterIp
) {
}
