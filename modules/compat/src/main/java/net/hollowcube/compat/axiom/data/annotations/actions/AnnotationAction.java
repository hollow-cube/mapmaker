package net.hollowcube.compat.axiom.data.annotations.actions;

import net.minestom.server.network.NetworkBuffer;

public interface AnnotationAction {

    NetworkBuffer.Type<AnnotationAction> SERIALIZER = NetworkBuffer.BYTE.unionType(AnnotationAction::getNetworkType, AnnotationAction::getNetworkId);

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static NetworkBuffer.Type<AnnotationAction> getNetworkType(byte id) {
        NetworkBuffer.Type type = switch (id) {
            case 0 -> CreateAnnotationAction.SERIALIZER;
            case 1 -> DeleteAnnotationAction.SERIALIZER;
            case 2 -> MoveAnnotationAction.SERIALIZER;
            case 3 -> ClearAnnotationAction.SERIALIZER;
            case 4 -> RotateAnnotationAction.SERIALIZER;
            default -> throw new IllegalArgumentException("Unknown annotation action type: " + id);
        };
        return (NetworkBuffer.Type<AnnotationAction>) type;
    }

    private static byte getNetworkId(AnnotationAction action) {
        return switch (action) {
            case CreateAnnotationAction ignored -> 0;
            case DeleteAnnotationAction ignored -> 1;
            case MoveAnnotationAction ignored -> 2;
            case ClearAnnotationAction ignored -> 3;
            case RotateAnnotationAction ignored -> 4;
            default -> throw new IllegalArgumentException("Unknown annotation action type: " + action);
        };
    }
}
