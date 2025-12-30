package net.hollowcube.mapmaker.player.responses;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.player.PlayerServiceImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record SendFriendRequestResult(
    boolean isRequest, @Nullable PlayerServiceImpl.PlayerServiceError error,
    @Nullable LimitError limitError
) {

    public boolean successful() {
        return this.error == null;
    }

    public record LimitError(
        @NotNull String code,
        @NotNull String message,
        int limit,
        int friendCount,
        int outgoingRequestCount
    ) {}
}
