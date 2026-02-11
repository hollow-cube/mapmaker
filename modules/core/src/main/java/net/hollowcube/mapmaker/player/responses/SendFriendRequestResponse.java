package net.hollowcube.mapmaker.player.responses;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record SendFriendRequestResponse(boolean isRequest) {
}
