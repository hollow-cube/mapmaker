package net.hollowcube.mapmaker.notifications;

import io.nats.client.Message;
import io.nats.client.MessageConsumer;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import net.hollowcube.mapmaker.gui.notifications.ToastManager;
import net.hollowcube.mapmaker.notifications.impl.PlayerNotificationType;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.hollowcube.mapmaker.util.nats.JetStreamWrapper;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;

import java.io.Closeable;
import java.time.Duration;
import java.util.UUID;

public class NotificationsConsumer implements Closeable {

    private static final String STREAM = "NOTIFICATIONS";
    private static final ConsumerConfiguration CONSUMER_CONFIG = ConsumerConfiguration.builder()
        .filterSubjects("notification.>")
        .deliverPolicy(DeliverPolicy.New)
        .ackPolicy(AckPolicy.None)
        .inactiveThreshold(Duration.ofMinutes(5))
        .build();

    private final ServiceContext services;

    private final MessageConsumer consumer;

    public NotificationsConsumer(ServiceContext services, JetStreamWrapper jetStream) {
        this.services = services;

        this.consumer = jetStream.subscribe(STREAM, CONSUMER_CONFIG,
            PlayerNotificationResponse.SimpleEntry.class,
            this::handleNotification);
    }

    @Override
    public void close() {
        try {
            consumer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleNotification(Message msg, PlayerNotificationResponse.SimpleEntry entry) {
        var uuid = UUID.fromString(entry.player());
        var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid);
        if (player == null) return;
        if (!entry.action().equals("create")) return;

        var type = PlayerNotificationType.Lookup.get(entry.type());
        if (type == null) return;

        var toast = type.createToast(player, this.services, entry);
        if (toast == null) return;

        ToastManager.showNotification(
            player,
            Component.translatable("gui.notification.toast"),
            toast
        );
    }
}
