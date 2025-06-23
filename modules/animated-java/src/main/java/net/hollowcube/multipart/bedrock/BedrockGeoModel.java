package net.hollowcube.multipart.bedrock;

import net.hollowcube.aj.util.AJCodecUtil;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record BedrockGeoModel(
        @NotNull Description description,
        @NotNull List<Bone> bones
) {
    public static final StructCodec<BedrockGeoModel> CODEC = StructCodec.struct(
            "description", Description.CODEC, BedrockGeoModel::description,
            "bones", Bone.CODEC.list(), BedrockGeoModel::bones,
            BedrockGeoModel::new);

    public record Description(int textureWidth, int textureHeight) {
        public static final StructCodec<Description> CODEC = StructCodec.struct(
                "texture_width", Codec.INT, Description::textureWidth,
                "texture_height", Codec.INT, Description::textureHeight,
                Description::new);
    }

    public record Bone(
            @NotNull String name,
            @Nullable String parent,
            @NotNull Vec pivot,
            @NotNull Vec rotation,
            @NotNull List<Cube> cubes
    ) {
        public static final StructCodec<Bone> CODEC = StructCodec.struct(
                "name", Codec.STRING, Bone::name,
                "parent", Codec.STRING.optional(), Bone::name,
                "pivot", AJCodecUtil.VEC, Bone::pivot,
                "rotation", AJCodecUtil.VEC.optional(Vec.ZERO), Bone::pivot,
                "cubes", Cube.CODEC.list(), Bone::cubes,
                Bone::new);
    }

    public record Cube(
            @NotNull Vec origin,
            @NotNull Vec size,
            @NotNull Vec pivot,
            @Nullable Vec rotation,
            float inflate,
            @NotNull Map<String, Uv> uv
    ) {
        public static final StructCodec<Cube> CODEC = StructCodec.struct(
                "origin", AJCodecUtil.VEC, Cube::origin,
                "size", AJCodecUtil.VEC, Cube::size,
                "pivot", AJCodecUtil.VEC.optional(Vec.ZERO), Cube::size,
                "rotation", AJCodecUtil.VEC.optional(), Cube::size,
                "inflate", Codec.FLOAT.optional(0f), Cube::inflate,
                "uv", Codec.STRING.mapValue(Uv.CODEC), Cube::uv,
                Cube::new);
    }

    public record Uv(
            float[] uv,
            float[] uvSize
    ) {
        public static final StructCodec<Uv> CODEC = StructCodec.struct(
                "uv", AJCodecUtil.FLOAT2, Uv::uv,
                "uv_size", AJCodecUtil.FLOAT2, Uv::uvSize,
                Uv::new);
    }

}
