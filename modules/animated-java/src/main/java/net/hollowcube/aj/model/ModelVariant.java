package net.hollowcube.aj.model;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.aj.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ModelVariant(
        boolean isDefault,
        @NotNull String name,
        @NotNull String displayName,
        // A map of default texture UUID -> variant texture UUID. If a texture is not in this map,
        // it will be assumed to be the same as the default texture.
        @NotNull Map<UUID, UUID> textureMap,
        @NotNull Map<UUID, ModelContainer> models,
        // A list of node UUIDs that should be excluded / ignored when this variant is applied.
        @NotNull List<UUID> excludedNodes
) {
    public static final Codec<ModelVariant> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("is_default", false).forGetter(ModelVariant::isDefault),
            Codec.STRING.fieldOf("name").forGetter(ModelVariant::name),
            Codec.STRING.fieldOf("display_name").forGetter(ModelVariant::displayName),
            Codec.unboundedMap(ExtraCodecs.UUID_STRING, ExtraCodecs.UUID_STRING).fieldOf("texture_map").forGetter(ModelVariant::textureMap),
            Codec.unboundedMap(ExtraCodecs.UUID_STRING, ModelContainer.CODEC).fieldOf("models").forGetter(ModelVariant::models),
            ExtraCodecs.UUID_STRING.listOf().fieldOf("excluded_nodes").forGetter(ModelVariant::excludedNodes)
    ).apply(i, ModelVariant::new));

    public record ModelContainer(
            @NotNull Model model,
            int customModelData
    ) {
        public static final Codec<ModelContainer> CODEC = RecordCodecBuilder.create(i -> i.group(
                Model.CODEC.fieldOf("model").forGetter(ModelContainer::model),
                Codec.INT.fieldOf("custom_model_data").forGetter(ModelContainer::customModelData)
        ).apply(i, ModelContainer::new));
    }

    public sealed interface Model {

        Codec<Model> CODEC = Codec.of(Model::encode, Model::decode);

        private static <T> DataResult<Pair<Model, T>> decode(DynamicOps<T> dynamicOps, T t) {
            Codec<? extends Model> codec = NullModel.CODEC;
            if (dynamicOps.get(t, "elements").result().isPresent())
                codec = VanillaModel.CODEC;
            else if (dynamicOps.get(t, "parent").result().isPresent())
                codec = VariantModel.CODEC;
            return codec.decode(dynamicOps, t).map(pair -> pair.mapFirst(model -> (Model) model));
        }

        private static <T> DataResult<T> encode(Model model, DynamicOps<T> dynamicOps, T t) {
            return switch (model) {
                case NullModel n -> NullModel.CODEC.encode(n, dynamicOps, t);
                case VanillaModel v -> VanillaModel.CODEC.encode(v, dynamicOps, t);
                case VariantModel v -> VariantModel.CODEC.encode(v, dynamicOps, t);
            };
        }

    }

    public static final class NullModel implements Model {
        public static final NullModel INSTANCE = new NullModel();

        public static final Codec<NullModel> CODEC = Codec.unit(INSTANCE);

        private NullModel() {
        }
    }

    public record VanillaModel(
            @NotNull JsonObject model
    ) implements Model {
        public static final Codec<VanillaModel> CODEC = ExtraCodecs.JSON_OBJECT
                .xmap(VanillaModel::new, VanillaModel::model);
    }

    public record VariantModel(
            @NotNull String parent,
            @NotNull Map<String, String> textures
    ) implements Model {
        public static final Codec<VariantModel> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("parent").forGetter(VariantModel::parent),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("textures").forGetter(VariantModel::textures)
        ).apply(i, VariantModel::new));
    }
}
