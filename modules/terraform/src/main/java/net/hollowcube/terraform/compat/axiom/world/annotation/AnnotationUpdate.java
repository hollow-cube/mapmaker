package net.hollowcube.terraform.compat.axiom.world.annotation;

import net.hollowcube.common.math.Quaternion;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public sealed interface AnnotationUpdate {
    @NotNull NetworkBuffer.Type<AnnotationUpdate> NETWORK_TYPE = NetworkBuffer.BYTE
            .unionType(AnnotationUpdate::networkType, AnnotationUpdate::typeId);

    record Create(@NotNull UUID uuid, @NotNull AnnotationData data) implements AnnotationUpdate {
        public static final NetworkBuffer.Type<Create> NETWORK_TYPE = NetworkBufferTemplate.template(
                NetworkBuffer.UUID, Create::uuid,
                AnnotationData.NETWORK_TYPE, Create::data,
                Create::new);
    }

    record Delete(@NotNull UUID uuid) implements AnnotationUpdate {
        public static final NetworkBuffer.Type<Delete> NETWORK_TYPE = NetworkBufferTemplate.template(
                NetworkBuffer.UUID, Delete::uuid,
                Delete::new);
    }

    record Move(@NotNull UUID uuid, @NotNull Point to) implements AnnotationUpdate {
        public static final NetworkBuffer.Type<Move> NETWORK_TYPE = NetworkBufferTemplate.template(
                NetworkBuffer.UUID, Move::uuid,
                NetworkBuffer.VECTOR3, Move::to,
                Move::new);
    }

    final class ClearAll implements AnnotationUpdate {
        public static final ClearAll INSTANCE = new ClearAll();

        public static final NetworkBuffer.Type<ClearAll> NETWORK_TYPE = NetworkBufferTemplate.template(
                ClearAll::new);

        private ClearAll() {
        }
    }

    record Rotate(@NotNull UUID uuid, @NotNull Quaternion quaternion) implements AnnotationUpdate {
        public static final NetworkBuffer.Type<Rotate> NETWORK_TYPE = NetworkBufferTemplate.template(
                NetworkBuffer.UUID, Rotate::uuid,
                Quaternion.FLOAT_NETWORK_TYPE, Rotate::quaternion,
                Rotate::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @NotNull NetworkBuffer.Type<AnnotationUpdate> networkType(byte type) {
        return (NetworkBuffer.Type) switch (type) {
            case 0 -> Create.NETWORK_TYPE;
            case 1 -> Delete.NETWORK_TYPE;
            case 2 -> Move.NETWORK_TYPE;
            case 3 -> ClearAll.NETWORK_TYPE;
            case 4 -> Rotate.NETWORK_TYPE;
            default -> throw new IllegalArgumentException("Unknown annotation update type: " + type);
        };
    }

    private static byte typeId(@NotNull AnnotationUpdate update) {
        return switch (update) {
            case Create ignored -> 0;
            case Delete ignored -> 1;
            case Move ignored -> 2;
            case ClearAll ignored -> 3;
            case Rotate ignored -> 4;
        };
    }
}
