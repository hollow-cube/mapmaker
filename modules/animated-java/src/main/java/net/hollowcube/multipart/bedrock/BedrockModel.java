package net.hollowcube.multipart.bedrock;

import net.hollowcube.aj.util.AJCodecUtil;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Either;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public record BedrockModel(
        @NotNull List<Texture> textures,
        @NotNull List<Element> elements,
        @NotNull List<OutlineElement> outliner
) {
    public static final StructCodec<BedrockModel> CODEC = StructCodec.struct(
            "textures", Texture.CODEC.list(), BedrockModel::textures,
            "elements", Element.CODEC.list(), BedrockModel::elements,
            "outliner", OutlineElement.CODEC.list(), BedrockModel::outliner,
            BedrockModel::new);

    public record Texture(
            @NotNull String name,
            @NotNull String source
    ) {
        public static final StructCodec<Texture> CODEC = StructCodec.struct(
                "name", Codec.STRING, Texture::name,
                "source", Codec.STRING, Texture::source,
                Texture::new);
    }

    public record Element(
            @NotNull UUID uuid,
            @NotNull Vec rotation,
            @NotNull Vec origin
    ) {
        public static final StructCodec<Element> CODEC = StructCodec.struct(
                "uuid", Codec.UUID_COERCED, Element::uuid,
                "rotation", AJCodecUtil.VEC.optional(Vec.ZERO), Element::rotation,
                "origin", AJCodecUtil.VEC.optional(Vec.ZERO), Element::origin,
                Element::new);
    }

    public record OutlineElement(
            @NotNull UUID uuid,
            @NotNull Vec rotation,
            @NotNull Vec origin,
            @NotNull List<Either<UUID, OutlineElement>> children
    ) {
        public static final Codec<OutlineElement> CODEC = Codec.Recursive(self -> StructCodec.struct(
                "uuid", Codec.UUID_COERCED, OutlineElement::uuid,
                "rotation", AJCodecUtil.VEC.optional(Vec.ZERO), OutlineElement::rotation,
                "origin", AJCodecUtil.VEC.optional(Vec.ZERO), OutlineElement::origin,
                "children", Codec.Either(Codec.UUID_COERCED, self).list().optional(List.of()), OutlineElement::children,
                OutlineElement::new));
    }
}
