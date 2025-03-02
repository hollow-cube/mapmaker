package net.hollowcube.compat.axiom.data.annotations.actions;

import net.hollowcube.compat.axiom.data.annotations.data.AnnotationData;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record CreateAnnotationAction(
        @NotNull UUID id,
        @NotNull AnnotationData data
) implements AnnotationAction {

    public static final NetworkBuffer.Type<CreateAnnotationAction> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, CreateAnnotationAction::id,
            AnnotationData.SERIALIZER, CreateAnnotationAction::data,
            CreateAnnotationAction::new
    );

}
