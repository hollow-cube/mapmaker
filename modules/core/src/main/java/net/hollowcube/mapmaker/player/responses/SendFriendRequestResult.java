package net.hollowcube.mapmaker.player.responses;

import net.hollowcube.mapmaker.player.PlayerServiceImpl;
import org.jetbrains.annotations.Nullable;

public record SendFriendRequestResult(
    boolean successful, boolean isRequest, @Nullable PlayerServiceImpl.PlayerServiceError error
) {}
