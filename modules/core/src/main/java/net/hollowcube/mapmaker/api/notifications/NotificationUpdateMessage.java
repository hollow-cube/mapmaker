package net.hollowcube.mapmaker.api.notifications;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record NotificationUpdateMessage(
    Action action
    // TODO
) {

    public enum Action {
        CREATE,
        DELETE,
    }
}
