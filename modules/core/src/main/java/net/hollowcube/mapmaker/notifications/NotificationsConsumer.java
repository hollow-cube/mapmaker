package net.hollowcube.mapmaker.notifications;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.gui.notifications.ToastManager;
import net.hollowcube.mapmaker.kafka.BaseConsumer;
import net.hollowcube.mapmaker.notifications.impl.PlayerNotificationType;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.ConnectionManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.UUID;

public class NotificationsConsumer extends BaseConsumer<PlayerNotificationResponse.SimpleEntry> {

    private static final String TOPIC_NAME = "notification_update";
    private static final String GROUP_ID = ServerRuntime.getRuntime().hostname();
    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    private final ServiceContext services;

    public NotificationsConsumer(String bootstrapServers, ServiceContext services) {
        super(TOPIC_NAME, GROUP_ID, it -> AbstractHttpService.GSON.fromJson(it, PlayerNotificationResponse.SimpleEntry.class), bootstrapServers);

        this.services = services;
    }

    @Override
    protected void onMessage(ConsumerRecord<String, String> record, PlayerNotificationResponse.SimpleEntry entry) {
        FutureUtil.submitVirtual(() -> {
            var uuid = UUID.fromString(record.key());
            var player = CONNECTION_MANAGER.getOnlinePlayerByUuid(uuid);
            if (player == null) return;
            if (!entry.action().equals("create")) return;

            var type = PlayerNotificationType.Lookup.get(entry.type());
            if (type == null) return;

            var toast = type.createToast(player, this.services, entry);

            ToastManager.showNotification(
                player,
                Component.translatable("gui.notification.toast"),
                toast
            );
        });
    }
}
