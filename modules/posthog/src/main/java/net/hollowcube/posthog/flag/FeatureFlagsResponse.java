package net.hollowcube.posthog.flag;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record FeatureFlagsResponse(
        @NotNull List<FeatureFlag> flags
) {
    public static final Codec<FeatureFlagsResponse> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.list(FeatureFlag.CODEC).fieldOf("flags").forGetter(FeatureFlagsResponse::flags)
    ).apply(i, FeatureFlagsResponse::new));
}
