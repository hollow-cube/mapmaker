package net.hollowcube.mapmaker.notifications.impl;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.notifications.DefaultActions;
import net.hollowcube.mapmaker.notifications.PlayerNotification;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.List;

@AutoService(PlayerNotificationType.class)
public final class MapBuilderRemovedNotificationType implements PlayerNotificationType {

    @Override
    public String type() {
        return "map_builder_removed";
    }

    private static Component getMapName(Player player, ServiceContext context, PlayerNotificationResponse.Entry entry) {
        var mapId = entry.key();
        var map = context.maps().getMap(player.getUuid().toString(), mapId);
        return Component.text(map.name());
    }

    @Override
    public PlayerNotification createNotification(Player player, ServiceContext context, PlayerNotificationResponse.ComplexEntry entry) {
        var mapName = getMapName(player, context, entry);

        return new PlayerNotification(
            entry,
            MapBuilderInviteNotificationType.ICON,
            "gui.notification.map_builder.removed",
            List.of(mapName),
            List.of(DefaultActions.delete(player, context, entry))
        );
    }

    @Override
    public Component createToast(Player player, ServiceContext context, PlayerNotificationResponse.SimpleEntry entry) {
        var mapName = getMapName(player, context, entry);
        return Component.translatable("gui.notification.map_builder.removed.toast", mapName);
    }
}
