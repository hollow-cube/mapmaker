package net.hollowcube.compat.axiom.data.annotations.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minestom.server.network.NetworkBuffer;

public interface AnnotationData {

    NetworkBuffer.Type<AnnotationData> SERIALIZER = NetworkBuffer.BYTE.unionType(AnnotationData::getNetworkType, AnnotationData::getNetworkId);
    Codec<AnnotationData> CODEC = Codec.STRING.dispatch("type", AnnotationData::getCodecId, AnnotationData::getCodecType);

    default AnnotationData withPosition(float x, float y, float z) {
        return this;
    }

    default AnnotationData withRotation(float x, float y, float z, float w) {
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static NetworkBuffer.Type<AnnotationData> getNetworkType(byte id) {
        NetworkBuffer.Type type = switch (id) {
            case 0 -> LineAnnotation.SERIALIZER;
            case 1 -> TextAnnotation.SERIALIZER;
            case 2 -> ImageAnnotation.SERIALIZER;
            case 3 -> FreehandOutlineAnnotation.SERIALIZER;
            case 4 -> LinesOutlineAnnotation.SERIALIZER;
            case 5 -> BoxOutlineAnnotation.SERIALIZER;
            default -> throw new IllegalArgumentException("Unknown annotation data type: " + id);
        };

        return (NetworkBuffer.Type<AnnotationData>) type;
    }

    private static MapCodec<? extends AnnotationData> getCodecType(String type) {
        return switch (type) {
            case "line" -> LineAnnotation.CODEC;
            case "text" -> TextAnnotation.CODEC;
            case "image" -> ImageAnnotation.CODEC;
            case "freehand_outline" -> FreehandOutlineAnnotation.CODEC;
            case "lines_outline" -> LinesOutlineAnnotation.CODEC;
            case "box_outline" -> BoxOutlineAnnotation.CODEC;
            default -> throw new IllegalArgumentException("Unknown annotation data type: " + type);
        };
    }

    private static byte getNetworkId(AnnotationData data) {
        return switch (data) {
            case LineAnnotation ignored -> 0;
            case TextAnnotation ignored -> 1;
            case ImageAnnotation ignored -> 2;
            case FreehandOutlineAnnotation ignored -> 3;
            case LinesOutlineAnnotation ignored -> 4;
            case BoxOutlineAnnotation ignored -> 5;
            default -> throw new IllegalArgumentException("Unknown annotation data type: " + data);
        };
    }

    private static String getCodecId(AnnotationData data) {
        return switch (data) {
            case LineAnnotation ignored -> "line";
            case TextAnnotation ignored -> "text";
            case ImageAnnotation ignored -> "image";
            case FreehandOutlineAnnotation ignored -> "freehand_outline";
            case LinesOutlineAnnotation ignored -> "lines_outline";
            case BoxOutlineAnnotation ignored -> "box_outline";
            default -> throw new IllegalArgumentException("Unknown annotation data type: " + data);
        };
    }
}
