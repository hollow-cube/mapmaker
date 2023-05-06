package net.hollowcube.map.feature;

import com.google.auto.service.AutoService;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.map.lang.MapMessages;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.common.config.ConfigProvider;
import net.hollowcube.mapmaker.kafka.BaseConsumer;
import net.hollowcube.mapmaker.kafka.FriendlyProducer;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.model.kafka.SchematicMgmt;
import net.hollowcube.terraform.schem.Schematic;
import net.hollowcube.terraform.schem.SchematicReadException;
import net.hollowcube.terraform.schem.SchematicReader;
import net.hollowcube.terraform.session.PlayerSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@AutoService(FeatureProvider.class)
public class SchematicUploadFeatureProvider implements FeatureProvider {
    private static final ServerRuntime runtime = ServerRuntime.getRuntime();
    private static final System.Logger logger = System.getLogger(SchematicUploadFeatureProvider.class.getName());

    private SchematicUploadConsumer consumer = null;
    private FriendlyProducer producer = null;

    @Override
    public void init(@NotNull ConfigProvider config) {
        logger.log(System.Logger.Level.INFO, "(not) Initializing schematic upload provider...");

        var kafkaConfig = config.get(KafkaConfig.class);
        consumer = new SchematicUploadConsumer(kafkaConfig.bootstrapServersStr());
        producer = new FriendlyProducer(kafkaConfig.bootstrapServersStr());
    }

    @Override
    public void shutdown() {
        if (consumer != null) consumer.close();
        if (producer != null) producer.close();
    }

    private class SchematicUploadConsumer extends BaseConsumer<SchematicMgmt> {
        private static final System.Logger logger = System.getLogger(SchematicUploadConsumer.class.getName());

        private static final String TOPIC_NAME = "schematic-mgmt";
        private static final String GROUP_ID = ServerRuntime.getRuntime().hostname();

        private static final byte[] ERR_NOT_EDITING = "You are not currently editing a map.".getBytes(StandardCharsets.UTF_8);

        protected SchematicUploadConsumer(@NotNull String bootstrapServers) {
            super(TOPIC_NAME, GROUP_ID, SchematicMgmt::fromJson, bootstrapServers);
            setAutocommit(false);
        }

        @Override
        protected void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull SchematicMgmt msg) {
            if (msg.origin().equals(runtime.hostname()))
                return;
            logger.log(System.Logger.Level.INFO, "Received schematic upload message: {0}", msg);

            var player = MinecraftServer.getConnectionManager().getPlayer(UUID.fromString(msg.owner()));
            if (player == null) return;

            // Get the current world of the player, if it is not an editing world then do nothing.
            var world = MapWorld.forPlayerOptional(player);
            if (world == null || (world.flags() & MapWorld.FLAG_EDITING) == 0) {
                logger.log(System.Logger.Level.INFO, "Player {0} is not editing a map, ignoring schematic upload.", player.getUuid());
                respondAndForget(msg, ERR_NOT_EDITING);
                return;
            }

            Schematic schem;
            try {
                schem = SchematicReader.read(new ByteArrayInputStream(msg.dataArray()));
            } catch (SchematicReadException e) {
                logger.log(System.Logger.Level.ERROR, "Failed to read schematic from message: {0}", msg);
                respondAndForget(msg, e.getMessage().getBytes(StandardCharsets.UTF_8));
                return;
            }

            // Get their terraform session and set their clipboard
            var session = PlayerSession.forPlayer(player);
            session.setClipboard(schem);
            player.sendMessage(MapMessages.SCHEMATIC_UPLOAD_SUCCESS.with(Component.text(msg.name())));

            respondAndForget(msg, null);
        }

        private void respondAndForget(@NotNull SchematicMgmt msg, byte @Nullable [] error) {
            var result = new SchematicMgmt(
                    runtime.hostname(), Instant.now().toEpochMilli(),
                    SchematicMgmt.ACTION_UPLOAD, msg.id(), msg.name(),
                    msg.owner(), error
            );
            producer.produceAndForget(TOPIC_NAME, result.toJson());
        }
    }
}
