package net.hollowcube.aj.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.aj.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record NodeTransform(
        @NotNull List<Float> matrix,
        @NotNull DecomposedMatrix decomposed,
        float[] pos,
        float[] rot,
        float[] headRot,
        float[] scale
) {
    public static final Codec<NodeTransform> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.listOf().fieldOf("matrix").forGetter(NodeTransform::matrix),
            DecomposedMatrix.CODEC.fieldOf("decomposed").forGetter(NodeTransform::decomposed),
            ExtraCodecs.FLOAT_3.fieldOf("pos").forGetter(NodeTransform::pos),
            ExtraCodecs.FLOAT_3.fieldOf("rot").forGetter(NodeTransform::rot),
            ExtraCodecs.FLOAT_2.fieldOf("head_rot").forGetter(NodeTransform::headRot),
            ExtraCodecs.FLOAT_3.fieldOf("scale").forGetter(NodeTransform::scale)
    ).apply(i, NodeTransform::new));

    public record DecomposedMatrix(
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
