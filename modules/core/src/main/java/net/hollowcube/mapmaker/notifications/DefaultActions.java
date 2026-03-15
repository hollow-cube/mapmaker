package net.hollowcube.mapmaker.notifications;

import net.hollowcube.common.components.TranslatableBuilder;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.api.notifications.Notification;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class DefaultActions {

    private static final Sprite DELETE_ICON = new Sprite("icon2/1_1/trash_can");
    private static final Sprite LINK_ICON = new Sprite("icon2/1_1/external_link");
    private static final Sprite JOIN_ICON = new Sprite("icon2/1_1/joy_stick");

    public static PlayerNotification.Action delete(Player player, ServiceContext context, Notification entry) {
        return PlayerNotification.Action.of(
            DELETE_ICON,
            "gui.notification.action.delete.interaction",
            "gui.notification.action.delete",
            PlayerNotification.ActionExecutor
                .ofAsync(() -> context.api().notifications.delete(entry.id()))
                .withConfirmation()
                .withRefresh()
        );
    }

    @Nullable
    public static PlayerNotification.Action link(Player player, ServiceContext context, Notification entry) {
        var link = entry.data() != null ? entry.data().get("link").getAsString() : null;
        if (link == null) return null;

        return link(player, context, entry, link);
    }

    public static PlayerNotification.Action link(Player player, ServiceContext context, Notification entry, String link) {
        return PlayerNotification.Action.of(
            LINK_ICON,
            "gui.notification.action.link.interaction",
            "gui.notification.action.link",
            PlayerNotification.ActionExecutor
                .of(() -> {
                    FutureUtil.submitVirtual(() -> context.api().notifications.setReadStatus(entry.id(), true));
                    player.sendMessage(
                        TranslatableBuilder
                            .of("gui.notification.action.link.message")
                            .with(link)
                            .toComponent()
                    );
                    player.closeInventory();
                })
        );
    }

    public static PlayerNotification.Action join(Player player, ServiceContext context, Notification entry, MapData map) {
        return PlayerNotification.Action.of(
            JOIN_ICON,
            "gui.notification.action.join.interaction",
            "gui.notification.action.join",
            PlayerNotification.ActionExecutor
                .of(() -> {
                    FutureUtil.submitVirtual(() -> context.api().notifications.setReadStatus(entry.id(), true));
                    FutureUtil.submitVirtual(() -> context.bridge().joinMap(
                        player,
                        new ServerBridge.JoinConfig(
                            map.id(),
                            ServerBridge.JoinMapState.PLAYING,
                            "notification_join_map",
                            null
                        )
                    ));
                })
        );
    }
}
