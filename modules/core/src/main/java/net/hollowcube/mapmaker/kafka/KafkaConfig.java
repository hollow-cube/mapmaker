package net.hollowcube.mapmaker.kafka;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
public record KafkaConfig(
        @Setting("bootstrap_servers")
        String bootstrapServersStr
) {

    public @NotNull List<String> bootstrapServers() {
        return List.of(bootstrapServersStr.split(","));
    }
}
