package net.hollowcube.mapmaker.map.feature.edit;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.mapmaker.kafka.BaseConsumer;
import net.hollowcube.mapmaker.kafka.FriendlyProducer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.util.file.FileUploadEvent;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventDispatcher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class FileMgmtConsumer extends BaseConsumer<FileMgmtConsumer.Message> {
    private FriendlyProducer producer = null;

    private static final String TOPIC_NAME = "file-mgmt";
    private static final String GROUP_ID = ServerRuntime.getRuntime().hostname();

    private static final byte[] ERR_NOT_EDITING = "You must be editing a map to upload files!".getBytes(StandardCharsets.UTF_8);

    private static final int ACTION_UPLOAD = 0;

    public record Message(
            @NotNull String origin,
            @NotNull String correlationId,
            int action,
            String type,
            String name,
            String owner,
            byte[] data
    ) {
    }

    public FileMgmtConsumer(@NotNull String bootstrapServers) {
        super(TOPIC_NAME, GROUP_ID, raw -> AbstractHttpService.GSON.fromJson(raw, Message.class), bootstrapServers);
        setAutocommit(false);
    }

    @Override
    protected void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull FileMgmtConsumer.Message msg) {
        if (msg.origin().equals(GROUP_ID)) return;

        var player = MinecraftServer.getConnectionManager()
                .getOnlinePlayerByUuid(UUID.fromString(msg.owner()));
        if (player == null) return;

        // Get the current world of the player, if it is not an editing world then do nothing.
        var world = MapWorld.forPlayerOptional(player);
        if (!(world instanceof EditingMapWorld)) {
            var response = new Message(GROUP_ID, msg.correlationId, -1, null, null, null, ERR_NOT_EDITING);
            producer.produceAndForget(TOPIC_NAME, AbstractHttpService.GSON.toJson(response));
            return;
        }

        var event = new FileUploadEvent(player, msg.action, msg.type, msg.name, msg.data);
        EventDispatcher.call(event);

        Message response;
        if (!event.isHandled()) {
            response = new Message(GROUP_ID, msg.correlationId, -1, null, null, null, null);
        } else if (event.error() != null) {
            response = new Message(GROUP_ID, msg.correlationId, -1, null, null, null, event.error().getBytes(StandardCharsets.UTF_8));
        } else {
            response = new Message(GROUP_ID, msg.correlationId, msg.action, msg.type, msg.name, msg.owner, null); // Success
        }
        producer.produceAndForget(TOPIC_NAME, AbstractHttpService.GSON.toJson(response));
    }
}
