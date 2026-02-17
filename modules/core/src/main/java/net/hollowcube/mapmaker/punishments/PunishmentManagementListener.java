package net.hollowcube.mapmaker.punishments;

import com.google.gson.Gson;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.kafka.BaseConsumer;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.punishments.event.PunishmentCreatedEvent;
import net.hollowcube.mapmaker.punishments.event.PunishmentRevokedEvent;
import net.hollowcube.mapmaker.punishments.types.Punishment;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import net.hollowcube.mapmaker.punishments.types.PunishmentUpdateMessage;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventDispatcher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class PunishmentManagementListener extends BaseConsumer<PunishmentUpdateMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PunishmentManagementListener.class);

    private static final String PUNISHMENTS_TOPIC = "punishments";
    private static final Gson GSON = AbstractHttpService.GSON;

    private final PlayerService playerService;
    private final PermManager permManager;

    public PunishmentManagementListener(
        @NotNull PlayerService playerService,
        @NotNull PermManager permManager,
        @NotNull String kafkaBrokers
    ) {
        super(PUNISHMENTS_TOPIC, "punishments", PunishmentManagementListener::fromJson, kafkaBrokers);
        this.playerService = playerService;
        this.permManager = permManager;
    }

    private static @NotNull PunishmentUpdateMessage fromJson(@NotNull String json) {
        return GSON.fromJson(json, PunishmentUpdateMessage.class);
    }

    @Override
    protected void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull PunishmentUpdateMessage message) {
        FutureUtil.submitVirtual(() -> {
            switch (message.action()) {
                case CREATE -> handlePunishmentCreated(message.punishment());
                case REVOKE -> handlePunishmentRevoked(message.punishment());
            }
        });
    }

    private void handlePunishmentCreated(@NotNull Punishment punishment) {
        LOGGER.info("Received punishment created message: {}", punishment);

        MinecraftServer.getSchedulerManager().scheduleNextTick(
            () -> EventDispatcher.call(new PunishmentCreatedEvent(punishment)));

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

        player.kick(Component.text("You have been " + (type == PunishmentType.BAN ? "banned" : "kicked") + " from the server. Reason: " + punishment.comment()));
    }

    private void handlePunishmentRevoked(@NotNull Punishment punishment) {
        MinecraftServer.getSchedulerManager().scheduleNextTick(
            () -> EventDispatcher.call(new PunishmentRevokedEvent(punishment)));

        announcePunishmentUpdate(false, punishment);
    }

    private void announcePunishmentUpdate(boolean created, @NotNull Punishment punishment) {
        var typeName = punishment.type().name().toLowerCase(Locale.ROOT);
        var announcement = Component.translatable(
            created ? "punishment.staff_announce." + typeName + ".created" : "punishment.staff_announce." + typeName + ".revoked",
            playerService.getPlayerDisplayName2(punishment.playerId()),
            playerService.getPlayerDisplayName2(punishment.executorId()),
            Component.text(Objects.requireNonNullElse(punishment.ladderId(), punishment.comment()))
        );
        var permission = switch (punishment.type()) {
            case BAN -> PlatformPerm.BAN_PLAYER;
            case MUTE -> PlatformPerm.MUTE_PLAYER;
            case KICK -> PlatformPerm.KICK_PLAYER;
        };
        for (var player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            var staffMode = PlayerData.fromPlayer(player).getSetting(PlayerSettings.STAFF_MODE);
            if (!staffMode || !permManager.hasPlatformPermission(player, permission)) continue;

            player.sendMessage(announcement);
        }
    }
}
