package net.hollowcube.compat.axiom.data.annotations.actions;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record RotateAnnotationAction(
        @NotNull UUID id,
        float x,
        float y,
        float z,
        float w
) implements AnnotationAction {

    public static final NetworkBuffer.Type<RotateAnnotationAction> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, RotateAnnotationAction::id,
            NetworkBuffer.FLOAT, RotateAnnotationAction::x,
            NetworkBuffer.FLOAT, RotateAnnotationAction::y,
            NetworkBuffer.FLOAT, RotateAnnotationAction::z,
            NetworkBuffer.FLOAT, RotateAnnotationAction::w,
            RotateAnnotationAction::new
    );

}
