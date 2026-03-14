package net.hollowcube.mapmaker.hub.item;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.gui.notifications.NotificationListView;
import net.hollowcube.mapmaker.gui.notifications.ToastManager;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class OpenNotificationsItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/notifications"));

    public static final Key ID = Key.key("mapmaker:notifications");
    public static final OpenNotificationsItem INSTANCE = new OpenNotificationsItem();

    private OpenNotificationsItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(Click click) {
        var world = MapWorld.forPlayer(click.player());
        if (world == null) return; // Sanity

        Panel.open(click.player(), new NotificationListView(world.server().createServiceContext()));
    }

    public static void checkForUnread(MapWorld world, WeakReference<Player> reference) {
        var playerId = OpUtils.map(reference.get(), it -> it.getUuid().toString());
        var players = world.server().playerService();

        if (playerId == null) return;

        FutureUtil.submitVirtual(() -> {
            var response = players.getNotifications(playerId, 0, true);
            if (response.results().isEmpty()) return;
            var hasOne = response.results().size() == 1 && response.pageCount() <= 1;
            var amount = response.results().size() + (response.pageCount() > 1 ? "+" : "");

            var player = reference.get();
            if (player == null) return;
            player.scheduleNextTick(entity -> {
                if (!(entity instanceof Player target)) return;
                ToastManager.showNotification(
                    target,
                    Component.translatable("gui.notification.toast"),
                    Component.translatable("gui.notification.toast.unread" + (hasOne ? "" : ".multiple"), Component.text(amount))
                );
            });
        });
    }
}
