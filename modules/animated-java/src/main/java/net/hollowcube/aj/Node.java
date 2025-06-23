package net.hollowcube.aj;

import net.kyori.adventure.key.Key;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public sealed interface Node {
    @NotNull Registry<StructCodec<? extends Node>> REGISTRY = DynamicRegistry.fromMap(Key.key("aj:node"),
            Map.entry(Key.key("struct"), Struct.CODEC),
            Map.entry(Key.key("bone"), Bone.CODEC),
            Map.entry(Key.key("text_display"), TextDisplay.CODEC),
            Map.entry(Key.key("locator"), Locator.CODEC));
    @NotNull StructCodec<Node> CODEC = Codec.RegistryTaggedUnion(_ -> REGISTRY, Node::codec, "type");

    record Base(
            @NotNull UUID uuid,
            @NotNull String name,
            @Nullable UUID parent,
            @NotNull Transform.Default defaultTransform
    ) {
        private static final StructCodec<Base> CODEC = StructCodec.struct(
                "uuid", Codec.UUID_COERCED, Base::uuid,
                "name", Codec.STRING, Base::name,
                "parent", Codec.UUID_COERCED.optional(), Base::parent,
                "default_transform", Transform.Default.CODEC, Base::defaultTransform,
                Base::new);
    }

    record Struct(@NotNull Node.Base base) implements Node {
        private static final StructCodec<Struct> CODEC = StructCodec.struct(
                StructCodec.INLINE, Base.CODEC, Struct::base,
                Struct::new);

        @Override
        public @NotNull StructCodec<? extends Node> codec() {
            return CODEC;
        }
    }

    record Bone(@NotNull Node.Base base) implements Node {
        private static final StructCodec<Bone> CODEC = StructCodec.struct(
                StructCodec.INLINE, Base.CODEC, Bone::base,
                Bone::new);

        @Override
        public @NotNull StructCodec<? extends Node> codec() {
            return CODEC;
        }
    }

    record TextDisplay(
            @NotNull Base base,
            @NotNull String text,
            @NotNull String align, // todo enum
            boolean shadow,
            boolean seeThrough
    ) implements Node {
        private static final StructCodec<TextDisplay> CODEC = StructCodec.struct(
                StructCodec.INLINE, Base.CODEC, TextDisplay::base,
                "text", Codec.STRING, TextDisplay::text,
                "align", Codec.STRING, TextDisplay::align,
                "shadow", Codec.BOOLEAN, TextDisplay::shadow,
                "see_through", Codec.BOOLEAN, TextDisplay::seeThrough,
                TextDisplay::new);

        @Override
        public @NotNull StructCodec<? extends Node> codec() {
            return CODEC;
        }
    }

    record Locator(@NotNull Node.Base base) implements Node {
        private static final StructCodec<Locator> CODEC = StructCodec.struct(
                StructCodec.INLINE, Base.CODEC, Locator::base,
                Locator::new);

        @Override
        public @NotNull StructCodec<? extends Node> codec() {
            return CODEC;
        }
    }

    @NotNull Node.Base base();

    default @NotNull UUID uuid() {
        return base().uuid();
    }

    default @NotNull String name() {
        return base().name();
    }

    default @Nullable UUID parent() {
        return base().parent();
    }

    @NotNull StructCodec<? extends Node> codec();

}
