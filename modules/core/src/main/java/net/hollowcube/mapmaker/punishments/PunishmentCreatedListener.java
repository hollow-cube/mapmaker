package net.hollowcube.mapmaker.punishments;

import com.google.gson.Gson;
import net.hollowcube.mapmaker.kafka.BaseConsumer;
import net.hollowcube.mapmaker.punishments.types.PunishmentCreatedMessage;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class PunishmentCreatedListener extends BaseConsumer<PunishmentCreatedMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PunishmentCreatedListener.class);

    private static final String PUNISHMENTS_TOPIC = "punishments";
    private static final Gson GSON = AbstractHttpService.GSON;

    public PunishmentCreatedListener(@NotNull String kafkaBrokers) {
        super(PUNISHMENTS_TOPIC, "punishments", PunishmentCreatedListener::fromJson, kafkaBrokers);
    }

    private static @NotNull PunishmentCreatedMessage fromJson(@NotNull String json) {
        return GSON.fromJson(json, PunishmentCreatedMessage.class);
    }

    @Override
    protected void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull PunishmentCreatedMessage message) {
        Thread.startVirtualThread(() -> {
            LOGGER.info("Received punishment created message: {}", message);

            var punishment = message.punishment();

            // TODO: Broadcast punishment message to all staff?

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
        });
    }
}
