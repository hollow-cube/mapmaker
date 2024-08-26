package net.hollowcube.aj.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.aj.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ModelAnimation(
        @NotNull String name,
        @NotNull LoopMode loopMode,
        double duration,
        double loopDelay,
        @NotNull List<UUID> excludedNodes,
        // There is an "effects" key in the JSON which is not a uuid >:(
        @NotNull Map<String, List<ModelKeyframe>> animators
        // There is a uuid field here, but its not required so I'm not going to include it.
        // The UUID should be taken from the key in the animators map of ExportedModel.
) {
    public enum LoopMode {
        LOOP,
        ONCE,
        HOLD
    }

    public static final Codec<ModelAnimation> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(ModelAnimation::name),
            ExtraCodecs.enumString(LoopMode.class).fieldOf("loop_mode").forGetter(ModelAnimation::loopMode),
            Codec.DOUBLE.fieldOf("duration").forGetter(ModelAnimation::duration),
            Codec.DOUBLE.optionalFieldOf("loop_delay", 0.0).forGetter(ModelAnimation::loopDelay),
            ExtraCodecs.UUID_STRING.listOf().fieldOf("excluded_nodes").forGetter(ModelAnimation::excludedNodes),
            Codec.unboundedMap(Codec.STRING, ModelKeyframe.CODEC.listOf()).fieldOf("animators").forGetter(ModelAnimation::animators)
    ).apply(i, ModelAnimation::new));
}
