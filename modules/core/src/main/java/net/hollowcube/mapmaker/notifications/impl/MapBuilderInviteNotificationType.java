package net.hollowcube.mapmaker.notifications.impl;

import com.google.auto.service.AutoService;
import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.api.maps.MapWriteMessages;
import net.hollowcube.mapmaker.api.notifications.Notification;
import net.hollowcube.mapmaker.notifications.PlayerNotification;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.List;

@AutoService(PlayerNotificationType.class)
public final class MapBuilderInviteNotificationType implements PlayerNotificationType {

    static final Sprite ICON = new Sprite("icon2/1_1/hammer");
    private static final Sprite ACCEPT_ICON = new Sprite("icon2/1_1/checkmark");
    private static final Sprite REJECT_ICON = new Sprite("icon2/1_1/cross");

    @Override
    public String type() {
        return "map_builder_invite";
    }

    private record EntryData(String mapId, Component mapName, Component inviterDisplayName) {
    }

    private static EntryData dataFromEntry(ServiceContext context, JsonObject data) {
        var inviterId = data.get("inviterId").getAsString();
        var mapId = data.get("mapId").getAsString();

        var inviterDisplayName = context.api().players.getDisplayName(inviterId);
        var map = context.api().maps.get(mapId);

        return new EntryData(map.id().toString(), Component.text(map.name()), inviterDisplayName.render());
    }

    @Override
    public PlayerNotification createNotification(Player player, ServiceContext context, Notification entry) {
        var data = dataFromEntry(context, entry.data());

        var playerId = player.getUuid().toString();
        return new PlayerNotification(
            entry,
            ICON,
            "gui.notification.map_builder.invite",
            List.of(data.inviterDisplayName(), data.mapName()),
            List.of(
                PlayerNotification.Action.of(
                    ACCEPT_ICON,
                    "gui.notification.map_builder.invite.action.accept",
                    "gui.notification.map_builder.invite.action.accept.tooltip",
                    PlayerNotification.ActionExecutor
                        .ofAsync(() -> {
                            var result = context.api().maps.acceptMapBuilderInvite(data.mapId(), playerId);
                            if (!result.succeeded()) player.sendMessage(MapWriteMessages.failure(result));
                        })
                        .withConfirmation("Accept Invite")
                        .withRefresh()
                ),
                PlayerNotification.Action.of(
                    REJECT_ICON,
                    "gui.notification.map_builder.invite.action.reject",
                    "gui.notification.map_builder.invite.action.reject.tooltip",
                    PlayerNotification.ActionExecutor
                        .ofAsync(() -> {
                            var result = context.api().maps.rejectMapBuilderInvite(data.mapId(), playerId);
                            if (!result.succeeded()) player.sendMessage(MapWriteMessages.failure(result));
                        })
                        .withConfirmation("Reject Invite")
                        .withRefresh()
                )
            )
        );
    }

    @Override
    public Component createToast(Player player, ServiceContext context, PlayerNotificationResponse.SimpleEntry entry) {
        var data = dataFromEntry(context, entry.data());
        return Component.translatable("gui.notification.map_builder.invite.toast", data.inviterDisplayName(), data.mapName());
    }
}
