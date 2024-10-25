package net.hollowcube.terraform.compat.axiom.packet.server;

import net.hollowcube.terraform.compat.axiom.packet.AxiomServerPacket;
import net.hollowcube.terraform.compat.axiom.world.annotation.AnnotationUpdate;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record AxiomAnnotationUpdatePacket(
        @NotNull List<AnnotationUpdate> updates
) implements AxiomServerPacket {
    public static final NetworkBuffer.Type<AxiomAnnotationUpdatePacket> SERIALIZER = NetworkBufferTemplate.template(
            AnnotationUpdate.NETWORK_TYPE.list(Short.MAX_VALUE), AxiomAnnotationUpdatePacket::updates,
            AxiomAnnotationUpdatePacket::new);

    public AxiomAnnotationUpdatePacket {
        updates = List.copyOf(updates);
    }
}
