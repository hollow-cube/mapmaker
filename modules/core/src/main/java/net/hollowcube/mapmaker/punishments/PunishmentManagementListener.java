package net.hollowcube.mapmaker.punishments;

import io.nats.client.Message;
import io.nats.client.MessageConsumer;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.punishments.event.PunishmentCreatedEvent;
import net.hollowcube.mapmaker.punishments.event.PunishmentRevokedEvent;
import net.hollowcube.mapmaker.punishments.types.Punishment;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import net.hollowcube.mapmaker.punishments.types.PunishmentUpdateMessage;
import net.hollowcube.mapmaker.util.nats.JetStreamWrapper;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class PunishmentManagementListener implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PunishmentManagementListener.class);

    private static final String STREAM = "PUNISHMENTS";
    private static final ConsumerConfiguration CONSUMER_CONFIG = ConsumerConfiguration.builder()
        .filterSubjects("punishment.>")
        .deliverPolicy(DeliverPolicy.New)
        .ackPolicy(AckPolicy.None)
        .inactiveThreshold(Duration.ofMinutes(5))
        .build();

    private final PlayerService playerService;

    private final MessageConsumer consumer;

    public PunishmentManagementListener(PlayerService playerService, JetStreamWrapper jetStream) {
        this.playerService = playerService;

        this.consumer = jetStream.subscribe(STREAM, CONSUMER_CONFIG, PunishmentUpdateMessage.class, this::handlePunishmentUpdate);
    }

    @Override
    public void close() {
        try {
            consumer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handlePunishmentUpdate(Message msg, PunishmentUpdateMessage message) {
        switch (message.action()) {
            case CREATE -> handlePunishmentCreated(message.punishment(), message.component());
            case REVOKE -> handlePunishmentRevoked(message.punishment());
        }
    }

    private void handlePunishmentCreated(Punishment punishment, Component message) {
        LOGGER.info("Received punishment created message: {}", punishment);

        MinecraftServer.getSchedulerManager().scheduleNextTick(
            () -> EventDispatcher.call(new PunishmentCreatedEvent(punishment))
        );

        // Announce async to kick as quick as possible
        FutureUtil.submitVirtual(() -> announcePunishmentUpdate(true, punishment));

        var type = punishment.type();
        if (type != PunishmentType.BAN && type != PunishmentType.KICK) {
            // Only bans and kicks require the target to be kicked
            return;
        }

        var playerId = UUID.fromString(punishment.playerId());
        var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerId);
        if (player == null) {
            // Player is not on this server - ignore
            return;
        }
        PlayerUtil.disconnect(player, message);
    }

    private void handlePunishmentRevoked(Punishment punishment) {
        MinecraftServer.getSchedulerManager().scheduleNextTick(
            () -> EventDispatcher.call(new PunishmentRevokedEvent(punishment)));

        announcePunishmentUpdate(false, punishment);
    }

    private void announcePunishmentUpdate(boolean created, Punishment punishment) {
        var typeName = punishment.type().name().toLowerCase(Locale.ROOT);
        var announcement = Component.translatable(
            created ? "punishment.staff_announce." + typeName + ".created" : "punishment.staff_announce." + typeName + ".revoked",
            playerService.getPlayerDisplayName2(punishment.playerId()),
            playerService.getPlayerDisplayName2(punishment.executorId()),
            Component.text(Objects.requireNonNullElse(punishment.ladderId(), punishment.comment()))
        );
        for (var player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            var playerData = PlayerData.fromPlayer(player);
            var staffMode = playerData.getSetting(PlayerSettings.STAFF_MODE);
            if (!staffMode || !playerData.has(Permission.GENERIC_STAFF)) continue;

            player.sendMessage(announcement);
        }
    }
}
