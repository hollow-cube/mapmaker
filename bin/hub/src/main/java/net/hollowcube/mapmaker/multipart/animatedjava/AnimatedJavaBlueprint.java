package net.hollowcube.mapmaker.multipart.animatedjava;

import net.kyori.adventure.key.Key;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import net.minestom.server.utils.Either;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record AnimatedJavaBlueprint(
    Meta meta,
    // Settings settings
    Resolution resolution,
    List<Element> elements,
    List<Texture> textures,
    List<Either<String, OutlineElement>> outliner
) {

    public static final StructCodec<AnimatedJavaBlueprint> CODEC = StructCodec.struct(
        "meta", Meta.CODEC, AnimatedJavaBlueprint::meta,
        "resolution", Resolution.CODEC, AnimatedJavaBlueprint::resolution,
        "elements", Element.CODEC.list(), AnimatedJavaBlueprint::elements,
        "textures", Texture.CODEC.list(), AnimatedJavaBlueprint::textures,
        "outliner", OutlineElement.CODEC.list(), AnimatedJavaBlueprint::outliner,
        AnimatedJavaBlueprint::new);

    public record Meta(
        String format,
        String formatVersion,
        String uuid
    ) {
        public static final StructCodec<Meta> CODEC = StructCodec.struct(
            "format", Codec.STRING, Meta::format,
            "format_version", Codec.STRING, Meta::formatVersion,
            "uuid", Codec.STRING, Meta::uuid,
            Meta::new);
    }

    public record Resolution(int width, int height) {
        public static final StructCodec<Resolution> CODEC = StructCodec.struct(
            "width", Codec.INT, Resolution::width,
            "height", Codec.INT, Resolution::height,
            Resolution::new);
    }

    public sealed interface Element {
        Codec<Element> CODEC = Codec.KEY.unionType(name -> switch (name.asString()) {
            case "animated_java:vanilla_block_display" -> BlockDisplay.CODEC;
            case "animated_java:vanilla_item_display" -> ItemDisplay.CODEC;
            case "animated_java:text_display" -> TextDisplay.CODEC;
            case "minecraft:cube" -> Cube.CODEC;
            default -> throw new IllegalArgumentException("Unknown element type: " + name);
        }, _ -> {
            throw new IllegalStateException("Cannot encode unknown element type");
        });

        @Nullable Transform transform();

        record Transform(
            Point position,
            Point rotation,
            Point scale
        ) {
            public static final StructCodec<Transform> CODEC = StructCodec.struct(
                "position", Codec.VECTOR3D, Transform::position,
                "rotation", Codec.VECTOR3D, Transform::rotation,
                "scale", Codec.VECTOR3D, Transform::scale,
                Transform::new);
        }

        record BlockDisplay(
            @Nullable Transform transform,
            String block
        ) implements Element {
            public static final StructCodec<BlockDisplay> CODEC = StructCodec.struct(
                StructCodec.INLINE, Transform.CODEC.optional(), BlockDisplay::transform,
                "block", Codec.STRING, BlockDisplay::block,
                BlockDisplay::new);
        }

        record ItemDisplay(
            @Nullable Transform transform,
            Key item
        ) implements Element {
            public static final StructCodec<ItemDisplay> CODEC = StructCodec.struct(
                StructCodec.INLINE, Transform.CODEC.optional(), ItemDisplay::transform,
                "item", Codec.KEY, ItemDisplay::item,
                ItemDisplay::new);
        }

        record TextDisplay(
            @Nullable Transform transform,
            String text
        ) implements Element {
            public static final StructCodec<TextDisplay> CODEC = StructCodec.struct(
                StructCodec.INLINE, Transform.CODEC.optional(), TextDisplay::transform,
                "text", Codec.STRING, TextDisplay::text,
                TextDisplay::new);
        }

        record Cube(
            @Nullable Transform transform,

            String uuid,
            String name,

            Point origin,
            Point from,
            Point to,
            Point rotation,
            int[] uvOffset,
            double inflate,
            Map<Direction, Face> faces,

            boolean rescale, // todo idk what these do
            int autoUv, // todo idk what these do
            boolean boxUv // todo idk what these do
        ) implements Element {
            public static final StructCodec<Cube> CODEC = StructCodec.struct(
                StructCodec.INLINE, Transform.CODEC.optional(), Cube::transform,
                "uuid", Codec.STRING, Cube::uuid,
                "name", Codec.STRING, Cube::name,
                "origin", Codec.VECTOR3D, Cube::origin,
                "from", Codec.VECTOR3D, Cube::from,
                "to", Codec.VECTOR3D, Cube::to,
                "rotation", Codec.VECTOR3D.optional(Vec.ZERO), Cube::rotation,
                "uv_offset", Codec.INT_ARRAY.optional(new int[2]), Cube::uvOffset,
                "inflate", Codec.DOUBLE.optional(0.0), Cube::inflate,
                "faces", Codec.Enum(Direction.class).mapValue(Face.CODEC), Cube::faces,
                "rescale", Codec.BOOLEAN, Cube::rescale,
                "autouv", Codec.INT, Cube::autoUv,
                "box_uv", Codec.BOOLEAN, Cube::boxUv,
                Cube::new);

            public record Face(int[] uv, int rotation, Codec.RawValue texture) {
                public static final StructCodec<Face> CODEC = StructCodec.struct(
                    "uv", Codec.INT_ARRAY, Face::uv,
                    "rotation", Codec.INT.optional(0), Face::rotation,
                    "texture", Codec.RAW_VALUE, Face::texture,
                    Face::new);
            }
        }

    }

    public record Texture(
        String uuid,
        String name,
        int width,
        int height,
        int uvWidth,
        int uvHeight,
        String source
    ) {
        public static final StructCodec<Texture> CODEC = StructCodec.struct(
            "uuid", Codec.STRING, Texture::uuid,
            "name", Codec.STRING, Texture::name,
            "width", Codec.INT, Texture::width,
            "height", Codec.INT, Texture::height,
            "uv_width", Codec.INT, Texture::uvWidth,
            "uv_height", Codec.INT, Texture::uvHeight,
            "source", Codec.STRING, Texture::source,
            Texture::new);
    }

    public record OutlineElement(
        String uuid,
        String name,
        Point origin,
        Point rotation,
        List<Either<String, OutlineElement>> children
    ) {
        public static final Codec<Either<String, OutlineElement>> CODEC = Codec.Recursive(self -> Codec.Either(
            Codec.STRING,
            StructCodec.struct(
                "uuid", Codec.STRING, OutlineElement::uuid,
                "name", Codec.STRING, OutlineElement::name,
                "origin", Codec.VECTOR3D, OutlineElement::origin,
                "rotation", Codec.VECTOR3D.optional(Vec.ZERO), OutlineElement::rotation,
                "children", self.list().optional(List.of()), OutlineElement::children,
                OutlineElement::new)
        ));
    }

}
