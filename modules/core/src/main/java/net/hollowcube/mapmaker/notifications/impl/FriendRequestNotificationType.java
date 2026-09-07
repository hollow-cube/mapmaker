package net.hollowcube.mapmaker.notifications.impl;

import com.google.auto.service.AutoService;
import net.hollowcube.ipc.notification.Notification;
import net.hollowcube.ipc.player.FriendRequestResult;
import net.hollowcube.mapmaker.command.relationship.SocialUtil;
import net.hollowcube.mapmaker.notifications.PlayerNotification;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.List;
import java.util.UUID;

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
        var sender = UUID.fromString(entry.key());
        var name = context.api().players.displayName(sender).render();

        return new PlayerNotification(
            entry,
            ICON,
            "gui.notification.friend_request",
            List.of(name),
            List.of(
                PlayerNotification.Action.of(
                    CONFIRM_ICON,
                    "gui.notification.friend_request.action.confirm.interaction",
                    "gui.notification.friend_request.action.confirm",
                    PlayerNotification.ActionExecutor
                        .of(() -> {
                            var result = context.api().social.sendFriendRequest(player.getUuid(), sender);
                            player.sendMessage(SocialUtil.friendRequestMessage(player, result, name));
                            // Sent means their request was already gone, so the inbox row is stale.
                            if (result instanceof FriendRequestResult.Sent || result instanceof FriendRequestResult.AlreadyFriends) {
                                context.api().notifications.delete(entry.id());
                            }
                        })
                        .withRefresh()
                ),
                PlayerNotification.Action.of(
                    REJECT_ICON,
                    "gui.notification.friend_request.action.reject.interaction",
                    "gui.notification.friend_request.action.reject",
                    PlayerNotification.ActionExecutor
                        .of(() -> {
                            if (context.api().social.deleteFriendRequest(player.getUuid(), sender, true) == null) {
                                context.api().notifications.delete(entry.id());
                            }
                        })
                        .withRefresh()
                )
            )
        );
    }

    @Override
    public Component createToast(Player player, ServiceContext context, PlayerNotificationResponse.SimpleEntry entry) {
        var username = context.api().players.displayName(UUID.fromString(entry.key())).render();
        return Component.translatable("gui.notification.friend_request.toast", username);
    }
}
