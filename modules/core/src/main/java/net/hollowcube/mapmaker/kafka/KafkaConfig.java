package net.hollowcube.mapmaker.kafka;

import net.hollowcube.common.config.ConfigPath;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigPath("kafka")
@ConfigSerializable
public record KafkaConfig(
        @Setting("bootstrap_servers")
        String bootstrapServersStr
) {

    public @NotNull List<String> bootstrapServers() {
        return List.of(bootstrapServersStr.split(","));
    }
}
