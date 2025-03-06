package net.hollowcube.mapmaker.cosmetic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;

public record CosmeticOptions(
        @NotNull String id,
        int color
) {

    public static final Codec<CosmeticOptions> CODEC = Codec.withAlternative(
            RecordCodecBuilder.create(i -> i.group(
                    Codec.STRING.fieldOf("id").forGetter(CosmeticOptions::id),
                    Codec.INT.optionalFieldOf("color", 0xFFFFFFFF).forGetter(CosmeticOptions::color)
            ).apply(i, CosmeticOptions::new)),
            Codec.STRING.xmap(CosmeticOptions::new, CosmeticOptions::id)
    );

    public CosmeticOptions(@NotNull String id) {
        this(id, 0xFFFFFFFF);
    }

    public boolean isEmpty() {
        return this.id.isEmpty();
    }

    public CosmeticOptions withColor(int color) {
        return new CosmeticOptions(this.id, color);
    }
}
