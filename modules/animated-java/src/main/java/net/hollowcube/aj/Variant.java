package net.hollowcube.aj;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public record Variant(
        @NotNull UUID uuid,
        boolean isDefault,
        @NotNull Map<UUID, Model> models
) {
    public static final StructCodec<Variant> CODEC = StructCodec.struct(
            "uuid", Codec.UUID_COERCED, Variant::uuid,
            "is_default", Codec.BOOLEAN.optional(false), Variant::isDefault,
            "models", Codec.UUID_COERCED.mapValue(Model.CODEC), Variant::models,
            Variant::new);

    public record Model(@NotNull Codec.RawValue model) {
        public static final StructCodec<Model> CODEC = StructCodec.struct(
                "model", Codec.RAW_VALUE, Model::model,
                Model::new);
    }
}
