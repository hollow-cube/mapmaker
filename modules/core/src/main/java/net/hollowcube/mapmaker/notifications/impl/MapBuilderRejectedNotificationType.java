package net.hollowcube.mapmaker.notifications.impl;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.notifications.DefaultActions;
import net.hollowcube.mapmaker.notifications.PlayerNotification;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.List;
import java.util.Objects;

@AutoService(PlayerNotificationType.class)
public final class MapBuilderRejectedNotificationType implements PlayerNotificationType {

    @Override
    public String type() {
        return "map_builder_rejected";
    }

    private record EntryData(boolean accepted, Component builderDisplayName, Component mapName) {
    }

    private static EntryData dataFromEntry(Player player, ServiceContext context, PlayerNotificationResponse.Entry entry) {
        var accepted = Boolean.parseBoolean(entry.key());

        var mapId = Objects.requireNonNull(entry.data()).get("mapId").getAsString();
        var builderId = Objects.requireNonNull(entry.data()).get("builderId").getAsString();

        var map = context.maps().getMap(player.getUuid().toString(), mapId);
        var builderDisplayName = context.players().getPlayerDisplayName2(builderId);

        return new EntryData(accepted, builderDisplayName.asComponent(), Component.text(map.name()));
    }

    @Override
    public PlayerNotification createNotification(Player player, ServiceContext context, PlayerNotificationResponse.ComplexEntry entry) {
        var data = dataFromEntry(player, context, entry);
        return new PlayerNotification(
            entry,
            MapBuilderInviteNotificationType.ICON,
            "gui.notification.map_builder.invite.reject",
            List.of(data.builderDisplayName(), data.mapName()),
            List.of(DefaultActions.delete(player, context, entry))
        );
    }

    @Override
    public Component createToast(Player player, ServiceContext context, PlayerNotificationResponse.SimpleEntry entry) {
        var data = dataFromEntry(player, context, entry);
        return Component.translatable(
            "gui.notification.map_builder.invite.reject.toast",
            data.builderDisplayName(),
            data.mapName()
        );
    }
}
