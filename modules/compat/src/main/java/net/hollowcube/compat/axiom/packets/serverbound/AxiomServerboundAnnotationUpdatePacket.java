package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.hollowcube.compat.axiom.data.annotations.actions.AnnotationAction;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.List;

public record AxiomServerboundAnnotationUpdatePacket(
    List<AnnotationAction> actions
) implements ServerboundModPacket<AxiomServerboundAnnotationUpdatePacket> {

    public static final Type<AxiomServerboundAnnotationUpdatePacket> TYPE = Type.of(
        AxiomAPI.CHANNEL, "annotation_update",
        NetworkBufferTemplate.template(
            AnnotationAction.SERIALIZER.list(), AxiomServerboundAnnotationUpdatePacket::actions,
            AxiomServerboundAnnotationUpdatePacket::new
        )
    );

    @Override
    public Type<AxiomServerboundAnnotationUpdatePacket> getType() {
        return TYPE;
    }
}
