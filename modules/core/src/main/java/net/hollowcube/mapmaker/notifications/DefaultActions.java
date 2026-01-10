package net.hollowcube.mapmaker.notifications;

import net.hollowcube.common.components.TranslatableBuilder;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class DefaultActions {

    private static final Sprite DELETE_ICON = new Sprite("icon2/1_1/trash_can");
    private static final Sprite LINK_ICON = new Sprite("icon2/1_1/external_link");

    public static PlayerNotification.Action delete(Player player, ServiceContext context, PlayerNotificationResponse.ComplexEntry entry) {
        return PlayerNotification.Action.of(
            DELETE_ICON,
            "gui.notification.action.delete.interaction",
            "gui.notification.action.delete",
            PlayerNotification.ActionExecutor
                .ofAsync(() -> context.players().deleteNotification(player.getUuid().toString(), entry.id()))
                .withConfirmation()
                .withRefresh()
        );
    }

    @Nullable
    public static PlayerNotification.Action link(Player player, ServiceContext context, PlayerNotificationResponse.ComplexEntry entry) {
        var link = entry.data() != null ? entry.data().get("link").getAsString() : null;
        if (link == null) return null;

        return link(player, context, entry, link);
    }

    public static PlayerNotification.Action link(Player player, ServiceContext context, PlayerNotificationResponse.ComplexEntry entry, String link) {
        return PlayerNotification.Action.of(
            LINK_ICON,
            "gui.notification.action.link.interaction",
            "gui.notification.action.link",
            PlayerNotification.ActionExecutor
                .of(() -> {
                    FutureUtil.submitVirtual(() -> context.players().markNotificationRead(player.getUuid().toString(), entry.id(), true));
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
}
