package net.hollowcube.compat.axiom.data.annotations.actions;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.UUID;

public record DeleteAnnotationAction(
        UUID id
) implements AnnotationAction {

    public static final NetworkBuffer.Type<DeleteAnnotationAction> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, DeleteAnnotationAction::id,
            DeleteAnnotationAction::new
    );

}
