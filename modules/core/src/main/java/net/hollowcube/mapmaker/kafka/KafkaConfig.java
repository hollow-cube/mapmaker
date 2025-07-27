package net.hollowcube.mapmaker.kafka;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RuntimeGson
public record KafkaConfig(
        String bootstrapServers
) {

    public @NotNull List<String> bootstrapServerList() {
        return List.of(bootstrapServers.split(","));
    }
}
