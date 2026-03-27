package net.hollowcube.mapmaker.notifications.impl;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.api.notifications.Notification;
import net.hollowcube.mapmaker.notifications.DefaultActions;
import net.hollowcube.mapmaker.notifications.PlayerNotification;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.List;

@AutoService(PlayerNotificationType.class)
public class UpdateNotificationType implements PlayerNotificationType {

    private static final Sprite ICON = new Sprite("icon2/1_1/newspaper");

    @Override
    public String type() {
        return "update";
    }

    @Override
    public PlayerNotification createNotification(Player player, ServiceContext context, Notification entry) {
        var link = DefaultActions.link(player, context, entry);
        return new PlayerNotification(
            entry,
            ICON,
            "gui.notification.update",
            List.of(Component.text(entry.key())),
            link != null ?
                List.of(link, DefaultActions.delete(player, context, entry)) :
                List.of(DefaultActions.delete(player, context, entry))
        );
    }

    @Override
    public Component createToast(Player player, ServiceContext context, PlayerNotificationResponse.SimpleEntry entry) {
        return null;
    }
}
