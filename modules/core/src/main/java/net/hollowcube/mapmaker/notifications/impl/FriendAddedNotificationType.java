package net.hollowcube.mapmaker.notifications.impl;

import com.google.auto.service.AutoService;
import net.hollowcube.ipc.notification.Notification;
import net.hollowcube.mapmaker.notifications.PlayerNotification;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.List;
import java.util.UUID;

@AutoService(PlayerNotificationType.class)
public class FriendAddedNotificationType implements PlayerNotificationType {

    private static final Sprite ICON = new Sprite("icon2/1_1/two_players");

    @Override
    public String type() {
        return "friend_added";
    }

    @Override
    public PlayerNotification createNotification(Player player, ServiceContext context, Notification entry) {
        return new PlayerNotification(
            entry,
            ICON,
            "gui.notification.friend_added",
            List.of(),
            List.of()
        );
    }

    @Override
    public Component createToast(Player player, ServiceContext context, PlayerNotificationResponse.SimpleEntry entry) {
        var username = context.api().players.displayName(UUID.fromString(entry.key())).render();
        return Component.translatable("gui.notification.friend_added.toast", username);
    }
}
