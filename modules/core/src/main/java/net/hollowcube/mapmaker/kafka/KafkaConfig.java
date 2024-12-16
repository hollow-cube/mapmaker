package net.hollowcube.mapmaker.kafka;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record KafkaConfig(
        String bootstrapServers
) {

    public @NotNull List<String> bootstrapServerList() {
        return List.of(bootstrapServers.split(","));
    }
}
