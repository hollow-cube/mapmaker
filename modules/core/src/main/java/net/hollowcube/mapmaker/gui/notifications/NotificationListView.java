package net.hollowcube.mapmaker.gui.notifications;

import net.hollowcube.common.components.TranslatableBuilder;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.notifications.PlayerNotification;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.utils.Unit;
import org.jetbrains.annotations.Blocking;

import java.util.ArrayList;
import java.util.List;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;

public class NotificationListView extends Panel {

    private static final Sprite DEFAULT_ICON = new Sprite("icon2/1_1/exclamation_mark", 1, 1);
    private static final List<String> ACTION_LORE = List.of(
        "gui.notifications.notification.lore.lmb",
        "gui.notifications.notification.lore.rmb"
    );

    private final Pagination<Unit> pagination;
    private final ServiceContext context;

    public NotificationListView(ServiceContext context) {
        super(9, 10);
        this.context = context;

        background("generic2/containers/paginated/7x3", -10, -31);
        add(0, 0, title("Notifications"));

        add(0, 0, backOrClose());

        this.pagination = add(1, 1, new Pagination<Unit>(7, 3)
            .fetchAsync(this::onSearch));
        add(2, 4, pagination.prevButton());
        add(3, 4, pagination.pageText(3, 1));
        add(6, 4, pagination.nextButton());
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);
        this.pagination.reset(Unit.INSTANCE);
    }

    @Blocking
    protected List<? extends Panel> onSearch(Unit ignored, int page, int pageSize) {
        var playerId = PlayerData.fromPlayer(this.host.player()).id();
        var notifications = this.context.players().getNotifications(playerId, page, false);

        if (notifications.page() == 0) {
            pagination.totalPages(notifications.pageCount());
        }

        return notifications
            .results()
            .stream()
            .map(entry -> {
                var notification = PlayerNotification.fromResponse(this.host.player(), this.context, entry);
                if (notification != null) {
                    return new NotificationElement(notification);
                }
                return new UnhandledNotificationElement(entry);
            })
            .toList();
    }

    private static class UnhandledNotificationElement extends Panel {

        protected UnhandledNotificationElement(PlayerNotificationResponse.ComplexEntry entry) {
            super(1, 1);

            var button = new Button("gui.notification.unhandled", 1, 1)
                .sprite(DEFAULT_ICON)
                .onLeftClick(() -> host.player().sendMessage(LanguageProviderV2.translateMultiMerged(
                        "gui.notification.unhandled.message",
                        List.of(Component.text(entry.id()))
                )));

            this.add(0, 0, button);
        }
    }

    private class NotificationElement extends Panel {

        private final PlayerNotification notification;

        protected NotificationElement(PlayerNotification notification) {
            super(1, 1);
            this.notification = notification;

            var host = NotificationListView.this.host;
            var context = NotificationListView.this.context;

            if (host == null) return;

            var actions = notification.actions();

            var button = new Button(1, 1)
                .sprite(notification.icon().withOffset(1, 1))
                .text(notification.title(), OpUtils.build(
                    new ArrayList<>(notification.body()),
                    lore -> {
                        lore.add(Component.empty());

                        for (var i = 0; i < actions.size(); i++) {
                            if (i >= ACTION_LORE.size()) break;
                            lore.add(
                                TranslatableBuilder.of(ACTION_LORE.get(i))
                                    .withTranslation(actions.get(i).interaction())
                                    .build()
                            );
                        }

                        lore.add(Component.translatable("gui.notifications.notification.lore.actions"));
                    }
                ))
                .onLeftClick(() -> {
                    this.readNotification();
                    if (actions.isEmpty()) return;
                    actions.getFirst().executeForRefresh(host, pagination::reset);
                })
                .onRightClick(() -> {
                    this.readNotification();
                    if (actions.size() < 2) return;
                    actions.get(1).executeForRefresh(host, pagination::reset);
                })
                .onShiftLeftClick(() -> {
                    this.readNotification();
                    host.pushView(new NotificationActionsView(notification, context));
                });

            this.add(0, 0, button);
        }

        private void readNotification() {
            if (notification.readAt() != null) return;

            var host = NotificationListView.this.host;
            var context = NotificationListView.this.context;
            if (host == null) return;

            var playerId = host.player().getUuid().toString();
            FutureUtil.submitVirtual(() -> context.players().markNotificationRead(playerId, notification.entry().id(), true));
        }
    }
}
