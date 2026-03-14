package net.hollowcube.compat.axiom.data.annotations.actions;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.UUID;

public record ClearAnnotationAction() implements AnnotationAction {

    public static final NetworkBuffer.Type<ClearAnnotationAction> SERIALIZER = NetworkBufferTemplate.template(
        ClearAnnotationAction::new
    );

}
