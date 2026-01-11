package net.hollowcube.mapmaker.notifications.impl;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.notifications.PlayerNotification;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.List;
import java.util.Objects;

@AutoService(PlayerNotificationType.class)
public class MapBuilderInviteNotificationType implements PlayerNotificationType {

    private static final Sprite ICON = new Sprite("icon2/1_1/hammer");
    private static final Sprite CONFIRM_ICON = new Sprite("icon2/1_1/checkmark");
    private static final Sprite REJECT_ICON = new Sprite("icon2/1_1/cross");

    @Override
    public String type() {
        return "map_builder_invite";
    }

    @Override
    public PlayerNotification createNotification(Player player, ServiceContext context, PlayerNotificationResponse.ComplexEntry entry) {
        var inviterId = Objects.requireNonNull(entry.data()).get("inviterId").getAsString();
        var mapId = Objects.requireNonNull(entry.data()).get("mapId").getAsString();

        var inviterDisplayName = context.players().getPlayerDisplayName2(inviterId);
        var map = context.maps().getMap(inviterId, mapId);

        // TODO: you should only be able to confirm if you have an available slot

        return new PlayerNotification(
            entry,
            ICON,
            "gui.notification.map_builder_invite",
            List.of(inviterDisplayName.asComponent(), Component.text(map.settings().getNameSafe())),
            List.of(
                PlayerNotification.Action.of(
                    CONFIRM_ICON,
                    "gui.notification.map_builder_invite.action.confirm.interaction",
                    "gui.notification.map_builder_invite.action.confirm",
                    PlayerNotification.ActionExecutor
                        .of(() -> {
                            context.maps().approveMapBuilder(mapId, player.getUuid().toString());
                        })
                        .withRefresh()
                ),
                PlayerNotification.Action.of(
                    REJECT_ICON,
                    "gui.notification.map_builder_invite.action.reject.interaction",
                    "gui.notification.map_builder_invite.action.reject",
                    PlayerNotification.ActionExecutor
                        .of(() -> {
                            // TODO: reject endpoint
//                            context.players().deleteFriendRequest(player.getUuid().toString(), entry.key(), true);
//                            context.players().deleteNotification(player.getUuid().toString(), entry.id());
                        })
                        .withRefresh()
                )
            )
        );
    }

    @Override
    public Component createToast(Player player, ServiceContext context, PlayerNotificationResponse.SimpleEntry entry) {
        return Component.translatable("gui.notification.map_builder_invite.toast"); //todo
    }
}
