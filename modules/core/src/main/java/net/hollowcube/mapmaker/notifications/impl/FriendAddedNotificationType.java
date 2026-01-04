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
public class FriendAddedNotificationType implements PlayerNotificationType {

    private static final BadSprite ICON = BadSprite.require("notifications/types/friend_request");

    @Override
    public String type() {
        return "friend_added";
    }

    @Override
    public PlayerNotification createNotification(Player player, ServiceContext context, PlayerNotificationResponse.ComplexEntry entry) {
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
        var username = context.players().getPlayerDisplayName2(entry.key());
        return Component.translatable("gui.notification.friend_added.toast", username);
    }
}
