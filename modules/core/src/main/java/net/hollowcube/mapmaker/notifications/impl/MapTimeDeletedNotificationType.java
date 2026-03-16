package net.hollowcube.mapmaker.notifications.impl;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.notifications.DefaultActions;
import net.hollowcube.mapmaker.notifications.PlayerNotification;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@AutoService(PlayerNotificationType.class)
public class MapTimeDeletedNotificationType implements PlayerNotificationType {

    private static final Sprite ICON = new Sprite("icon2/1_1/robber_running");

    @Override
    public String type() {
        return "map_time_deleted";
    }

    @Override
    public PlayerNotification createNotification(Player player, ServiceContext context, PlayerNotificationResponse.ComplexEntry entry) {
        var map = context.maps().getMap(player.getUuid().toString(), entry.key());

        return new PlayerNotification(
            entry,
            ICON,
            "gui.notification.map_time_deleted",
            List.of(Component.text(map.name())),
            List.of(DefaultActions.join(player, context, entry, map), DefaultActions.delete(player, context, entry))
        );
    }

    @Override
    public @Nullable Component createToast(Player player, ServiceContext context, PlayerNotificationResponse.SimpleEntry entry) {
        return null;
    }
}
