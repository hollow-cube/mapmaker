package net.hollowcube.mapmaker.notifications.impl;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.notifications.DefaultActions;
import net.hollowcube.mapmaker.notifications.PlayerNotification;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.List;

@AutoService(PlayerNotificationType.class)
public class RecapNotificationType implements PlayerNotificationType {

    private static final Sprite ICON = new Sprite("icon2/1_1/trophy");

    @Override
    public String type() {
        return "recap";
    }

    @Override
    public PlayerNotification createNotification(Player player, ServiceContext context, PlayerNotificationResponse.ComplexEntry entry) {
        var link = "https://recap.hollowcube.net/" + entry.key();
        return new PlayerNotification(
            entry,
            ICON,
            "gui.notification.recap",
            List.of(Component.text(entry.key())),
            List.of(
                DefaultActions.link(player, context, entry, link),
                DefaultActions.delete(player, context, entry)
            )
        );
    }

    @Override
    public Component createToast(Player player, ServiceContext context, PlayerNotificationResponse.SimpleEntry entry) {
        return null;
    }
}
