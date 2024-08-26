package net.hollowcube.aj.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.aj.util.ExtraCodecs;
import net.kyori.adventure.text.Component;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public sealed interface ModelNode permits ModelNode.Bone, ModelNode.Struct,
        ModelNode.Camera, ModelNode.Locator, ModelNode.TextDisplay,
        ModelNode.ItemDisplay, ModelNode.BlockDisplay {

    @NotNull BaseProps props();

    default @NotNull NodeType type() {
        return props().type();
    }

    default @NotNull String name() {
        return props().name();
    }

    default @NotNull UUID uuid() {
        return props().uuid();
    }

    default @Nullable String parent() {
        return props().parent().orElse(null);
    }

    Codec<ModelNode> CODEC = NodeType.CODEC.dispatch("type", ModelNode::type, NodeType::nodeCodec);

    record Bone(
            @NotNull BaseProps props,
            @NotNull Optional<String> optModelPath,
            // TODO: bounding_box
            @NotNull Map<UUID, BoneConfig> configs
    ) implements ModelNode {
        public static Codec<Bone> CODEC = RecordCodecBuilder.create(i -> i.group(
                BaseProps.CODEC.forGetter(Bone::props),
                Codec.STRING.optionalFieldOf("modelPath").forGetter(Bone::optModelPath),
                Codec.unboundedMap(ExtraCodecs.UUID_STRING, BoneConfig.CODEC).fieldOf("configs").forGetter(Bone::configs)
        ).apply(i, Bone::new));

        public @Nullable String modelPath() {
            return optModelPath.orElse(null);
        }
    }

    record Struct(@NotNull BaseProps props) implements ModelNode {
        public static Codec<Struct> CODEC = RecordCodecBuilder.create(i -> i.group(
                BaseProps.CODEC.forGetter(Struct::props)
        ).apply(i, Struct::new));
    }

    record Camera(@NotNull BaseProps props) implements ModelNode {
        public static Codec<Camera> CODEC = RecordCodecBuilder.create(i -> i.group(
                BaseProps.CODEC.forGetter(Camera::props)
        ).apply(i, Camera::new));
    }

    record Locator(@NotNull BaseProps props, @NotNull LocatorConfig config) implements ModelNode {
        public static Codec<Locator> CODEC = RecordCodecBuilder.create(i -> i.group(
                BaseProps.CODEC.forGetter(Locator::props),
                LocatorConfig.CODEC.fieldOf("config").forGetter(Locator::config)
        ).apply(i, Locator::new));
    }

    record TextDisplay(
            @NotNull BaseProps props,
            @NotNull Component text,
            @NotNull Optional<Integer> lineWidth,
            @NotNull Optional<String> backgroundColor,
            @NotNull Optional<Integer> backgroundAlpha,
            @NotNull Align align,
            @NotNull BoneConfig config
    ) implements ModelNode {
        public enum Align {
            LEFT,
            CENTER,
            RIGHT
        }

        public static Codec<TextDisplay> CODEC = RecordCodecBuilder.create(i -> i.group(
                BaseProps.CODEC.forGetter(TextDisplay::props),
                ExtraCodecs.TEXT_COMPONENT.fieldOf("text").forGetter(TextDisplay::text),
                Codec.INT.optionalFieldOf("line_width").forGetter(TextDisplay::lineWidth),
                Codec.STRING.optionalFieldOf("background_color").forGetter(TextDisplay::backgroundColor),
                Codec.INT.optionalFieldOf("background_alpha").forGetter(TextDisplay::backgroundAlpha),
                ExtraCodecs.enumString(Align.class).fieldOf("align").forGetter(TextDisplay::align),
                BoneConfig.CODEC.fieldOf("config").forGetter(TextDisplay::config)
        ).apply(i, TextDisplay::new));
    }

    record ItemDisplay(
            @NotNull BaseProps props,
            @NotNull Material item,
            @NotNull BoneConfig config
    ) implements ModelNode {
        public static Codec<ItemDisplay> CODEC = RecordCodecBuilder.create(i -> i.group(
                BaseProps.CODEC.forGetter(ItemDisplay::props),
                ExtraCodecs.MATERIAL.fieldOf("item").forGetter(ItemDisplay::item),
                BoneConfig.CODEC.fieldOf("config").forGetter(ItemDisplay::config)
        ).apply(i, ItemDisplay::new));
    }

    record BlockDisplay(
            @NotNull BaseProps props,
            @NotNull Block block,
            @NotNull BoneConfig config
    ) implements ModelNode {
        public static Codec<BlockDisplay> CODEC = RecordCodecBuilder.create(i -> i.group(
                BaseProps.CODEC.forGetter(BlockDisplay::props),
                ExtraCodecs.BLOCK.fieldOf("block").forGetter(BlockDisplay::block),
                BoneConfig.CODEC.fieldOf("config").forGetter(BlockDisplay::config)
        ).apply(i, BlockDisplay::new));
    }

    record BaseProps(
            @NotNull NodeType type,
            @NotNull String name,
            @NotNull UUID uuid,
            @NotNull Optional<String> parent,
            @NotNull DefaultTransform defaultTransform
    ) {
        public static MapCodec<BaseProps> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                NodeType.CODEC.fieldOf("type").forGetter(BaseProps::type),
                Codec.STRING.fieldOf("name").forGetter(BaseProps::name),
                Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("uuid").forGetter(BaseProps::uuid),
                Codec.STRING.optionalFieldOf("parent").forGetter(BaseProps::parent),
                DefaultTransform.CODEC.fieldOf("default_transform").forGetter(BaseProps::defaultTransform)
        ).apply(i, BaseProps::new));
    }

    record DefaultTransform(
            @NotNull List<Float> matrix,
            @NotNull DecomposedMatrix decomposed,
            float[] pos,
            float[] rot,
            float[] headRot,
            float[] scale
    ) {
        public static final Codec<DefaultTransform> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.listOf().fieldOf("matrix").forGetter(DefaultTransform::matrix),
                DecomposedMatrix.CODEC.fieldOf("decomposed").forGetter(DefaultTransform::decomposed),
                ExtraCodecs.FLOAT_3.fieldOf("pos").forGetter(DefaultTransform::pos),
                ExtraCodecs.FLOAT_3.fieldOf("rot").forGetter(DefaultTransform::rot),
                ExtraCodecs.FLOAT_2.fieldOf("head_rot").forGetter(DefaultTransform::headRot),
                ExtraCodecs.FLOAT_3.fieldOf("scale").forGetter(DefaultTransform::scale)
        ).apply(i, DefaultTransform::new));
    }

    record DecomposedMatrix(
            float[] translation,
            float[] leftRotation,
            float[] scale
    ) {
        public static final Codec<DecomposedMatrix> CODEC = RecordCodecBuilder.create(i -> i.group(
                ExtraCodecs.FLOAT_3.fieldOf("translation").forGetter(DecomposedMatrix::translation),
                ExtraCodecs.FLOAT_4.fieldOf("left_rotation").forGetter(DecomposedMatrix::leftRotation),
                ExtraCodecs.FLOAT_3.fieldOf("scale").forGetter(DecomposedMatrix::scale)
        ).apply(i, DecomposedMatrix::new));
    }
}
