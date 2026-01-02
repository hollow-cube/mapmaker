package net.hollowcube.mapmaker.gui.notifications;

import net.hollowcube.common.lang.TimeComponent;
import net.hollowcube.mapmaker.notifications.PlayerNotification;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.inventory.InventoryType;

import java.util.ArrayList;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;

public class NotificationActionsView extends Panel {

    private final PlayerNotification notification;

    public NotificationActionsView(PlayerNotification notification, ServiceContext services) {
        super(InventoryType.CHEST_4_ROW, 9, 4);

        this.notification = notification;

        var times = new ArrayList<Component>();
        times.add(Component.translatable("gui.notification.time.sent_at", TimeComponent.of(notification.createdAt())));
        if (notification.readAt() != null) times.add(
            Component.translatable("gui.notification.time.read_at", TimeComponent.of(notification.createdAt())));
        if (notification.expiresAt() != null) times.add(
            Component.translatable("gui.notification.time.expires_at", TimeComponent.of(notification.expiresAt(), true)));

        background("generic2/containers/7x1", -10, -31);
        add(0, 0, title("Notification"));

        add(0, 0, backOrClose());
        add(1, 0, info("notifications"));
        add(2, 0, new Text(null, 5, 1, "Notification")
            .background("generic2/btn/default/5_1")
            .align(Text.CENTER, Text.CENTER)
        );
        add(7, 0, new Button("gui.notification.unread", 1, 1)
            .background("notifications/unread")
            .onLeftClick(() -> services.players().markNotificationRead(
                this.host.player().getUuid().toString(),
                notification.entry().id(),
                false
            ))
        );
        add(8, 0, new Button(null, 1, 1)
            .background("notifications/time")
            .text(Component.translatable("gui.notification.time.name"), times)
        );
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);

        if (!isInitial) return;

        var actions = this.notification.actions();
        for (int i = 0; i < 7; i++) {
            Button button;

            if (i < actions.size()) {
                var action = actions.get(i);
                button = new Button(action.tooltip(), 1, 1)
                    .model(action.icon().model(), null)
                    .onLeftClick(() ->  action.executeForCompletion(this.host, host::popOrClose))
                    .background("generic2/btn/default/1_1ex");
            } else {
                button = new Button(1, 1).background("generic2/btn/disabled/1_1ex");
            }
            add(i + 1, 2, button);
        }
    }
}
