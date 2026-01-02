package net.hollowcube.mapmaker.notifications.impl;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.notifications.PlayerNotification;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.List;

@AutoService(PlayerNotificationType.class)
public class FriendRequestNotificationType implements PlayerNotificationType {

    private static final BadSprite ICON = BadSprite.require("notifications/types/friend_request");
    private static final BadSprite CONFIRM_ICON = BadSprite.require("notifications/actions/confirm");
    private static final BadSprite REJECT_ICON = BadSprite.require("notifications/actions/reject");

    @Override
    public String type() {
        return "friend_request";
    }

    @Override
    public PlayerNotification createNotification(Player player, ServiceContext context, PlayerNotificationResponse.ComplexEntry entry) {
        var username = context.players().getPlayerDisplayName2(entry.key());

        return new PlayerNotification(
            entry,
            ICON,
            "gui.notification.friend_request",
            List.of(username.asComponent()),
            false,
            List.of(
                PlayerNotification.Action.of(
                    CONFIRM_ICON,
                    "gui.notification.friend_request.action.confirm.interaction",
                    "gui.notification.friend_request.action.confirm",
                    PlayerNotification.ActionExecutor
                        .of(() -> {
                            context.players().sendFriendRequest(player.getUuid().toString(), entry.key());
                            context.players().deleteNotification(player.getUuid().toString(), entry.id());
                        })
                        .withRefresh()
                ),
                PlayerNotification.Action.of(
                    REJECT_ICON,
                    "gui.notification.friend_request.action.reject.interaction",
                    "gui.notification.friend_request.action.reject",
                    PlayerNotification.ActionExecutor
                        .of(() -> {
                            context.players().deleteFriendRequest(player.getUuid().toString(), entry.key(), true);
                            context.players().deleteNotification(player.getUuid().toString(), entry.id());
                        })
                        .withRefresh()
                )
            )
        );
    }

    @Override
    public Component createToast(Player player, ServiceContext context, PlayerNotificationResponse.SimpleEntry entry) {
        var username = context.players().getPlayerDisplayName2(entry.key());
        return Component.translatable("gui.notification.friend_request.toast", username);
    }
}
