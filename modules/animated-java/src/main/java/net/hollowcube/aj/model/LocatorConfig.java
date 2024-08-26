package net.hollowcube.aj.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;

public record LocatorConfig(
        boolean useEntity,
        @NotNull String entityType,
        @NotNull String summonCommands,
        @NotNull String tickingCommands
) {
    public static final Codec<LocatorConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("use_entity", false).forGetter(LocatorConfig::useEntity),
            Codec.STRING.optionalFieldOf("entity_type", "minecraft:pig").forGetter(LocatorConfig::entityType),
            Codec.STRING.optionalFieldOf("summon_commands", "").forGetter(LocatorConfig::summonCommands),
            Codec.STRING.optionalFieldOf("ticking_commands", "").forGetter(LocatorConfig::tickingCommands)
    ).apply(i, LocatorConfig::new));
}
