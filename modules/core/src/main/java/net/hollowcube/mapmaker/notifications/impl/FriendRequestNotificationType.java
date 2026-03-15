package net.hollowcube.mapmaker.notifications.impl;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.api.notifications.Notification;
import net.hollowcube.mapmaker.notifications.PlayerNotification;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.List;

@AutoService(PlayerNotificationType.class)
public class FriendRequestNotificationType implements PlayerNotificationType {

    private static final Sprite ICON = new Sprite("icon2/1_1/two_players");
    private static final Sprite CONFIRM_ICON = new Sprite("icon2/1_1/checkmark");
    private static final Sprite REJECT_ICON = new Sprite("icon2/1_1/cross");

    @Override
    public String type() {
        return "friend_request";
    }

    @Override
    public PlayerNotification createNotification(Player player, ServiceContext context, Notification entry) {
        var username = context.players().getPlayerDisplayName2(entry.key());

        return new PlayerNotification(
            entry,
            ICON,
            "gui.notification.friend_request",
            List.of(username.asComponent()),
            List.of(
                PlayerNotification.Action.of(
                    CONFIRM_ICON,
                    "gui.notification.friend_request.action.confirm.interaction",
                    "gui.notification.friend_request.action.confirm",
                    PlayerNotification.ActionExecutor
                        .of(() -> {
                            context.players().sendFriendRequest(player.getUuid().toString(), entry.key());
                            context.api().notifications.delete(entry.id());
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
                            context.api().notifications.delete(entry.id());
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
