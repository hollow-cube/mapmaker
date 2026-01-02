package net.hollowcube.mapmaker.notifications;

import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.minestom.server.entity.Player;

public final class DefaultActions {

    private static final BadSprite DELETE_ICON = BadSprite.require("notifications/actions/delete");

    public static PlayerNotification.Action delete(Player player, ServiceContext context, PlayerNotificationResponse.ComplexEntry entry) {
        return PlayerNotification.Action.of(
            DELETE_ICON,
            "gui.notification.action.delete.interaction",
            "gui.notification.action.delete",
            PlayerNotification.ActionExecutor
                .of(() -> context.players().deleteNotification(player.getUuid().toString(), entry.id()))
                .withConfirmation()
        );
    }
}
