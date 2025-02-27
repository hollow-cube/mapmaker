package net.hollowcube.compat.axiom.data.annotations.actions;

import net.hollowcube.compat.axiom.data.annotations.data.AnnotationData;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.UUID;

public record CreateAnnotationAction(
        UUID id,
        AnnotationData data
) implements AnnotationAction {

    public static final NetworkBuffer.Type<CreateAnnotationAction> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, CreateAnnotationAction::id,
            AnnotationData.SERIALIZER, CreateAnnotationAction::data,
            CreateAnnotationAction::new
    );

}
