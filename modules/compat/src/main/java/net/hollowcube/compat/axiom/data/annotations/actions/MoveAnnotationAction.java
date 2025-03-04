package net.hollowcube.compat.axiom.data.annotations.actions;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record MoveAnnotationAction(
        @NotNull UUID id,
        float x,
        float y,
        float z
) implements AnnotationAction {

    public static final NetworkBuffer.Type<MoveAnnotationAction> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, MoveAnnotationAction::id,
            NetworkBuffer.FLOAT, MoveAnnotationAction::x,
            NetworkBuffer.FLOAT, MoveAnnotationAction::y,
            NetworkBuffer.FLOAT, MoveAnnotationAction::z,
            MoveAnnotationAction::new
    );

}
