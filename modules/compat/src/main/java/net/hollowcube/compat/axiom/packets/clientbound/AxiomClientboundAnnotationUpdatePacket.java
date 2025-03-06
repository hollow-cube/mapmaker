package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.axiom.AxiomAPI;
import net.hollowcube.compat.axiom.data.annotations.actions.AnnotationAction;
import net.minestom.server.entity.GameMode;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.EnumSet;
import java.util.List;

public record AxiomClientboundAnnotationUpdatePacket(
    List<AnnotationAction> actions
) implements AxiomClientboundModPacket<AxiomClientboundAnnotationUpdatePacket> {

    public static final Type<AxiomClientboundAnnotationUpdatePacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "annotation_update",
            NetworkBufferTemplate.template(
                    AnnotationAction.SERIALIZER.list(256), AxiomClientboundAnnotationUpdatePacket::actions,
                    AxiomClientboundAnnotationUpdatePacket::new
            )
    );

    @Override
    public Type<AxiomClientboundAnnotationUpdatePacket> getType() {
        return TYPE;
    }
}
