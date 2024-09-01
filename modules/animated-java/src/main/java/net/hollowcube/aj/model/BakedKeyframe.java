package net.hollowcube.aj.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.aj.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record BakedKeyframe(
        double time,
        @NotNull Map<UUID, NodeTransform> nodeTransforms,
        @NotNull Optional<Variant> variant
) {

    public static final Codec<BakedKeyframe> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("time").forGetter(BakedKeyframe::time),
            Codec.unboundedMap(ExtraCodecs.UUID_STRING, NodeTransform.CODEC).fieldOf("node_transforms").forGetter(BakedKeyframe::nodeTransforms),
            Variant.CODEC.optionalFieldOf("variant").forGetter(BakedKeyframe::variant)
    ).apply(i, BakedKeyframe::new));

    public record Variant(
            @Nullable Optional<UUID> uuid,
            @Nullable Optional<String> executeCondition
    ) {
        public static final Codec<Variant> CODEC = RecordCodecBuilder.create(i -> i.group(
                ExtraCodecs.UUID_STRING.optionalFieldOf("uuid").forGetter(Variant::uuid),
                Codec.STRING.optionalFieldOf("execute_condition").forGetter(Variant::executeCondition)
        ).apply(i, Variant::new));
    }
    
}
