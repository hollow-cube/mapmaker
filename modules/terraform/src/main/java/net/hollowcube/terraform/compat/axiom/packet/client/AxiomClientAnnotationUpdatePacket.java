package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.hollowcube.terraform.compat.axiom.world.annotation.AnnotationUpdate;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record AxiomClientAnnotationUpdatePacket(
        @NotNull List<AnnotationUpdate> updates
) implements AxiomClientPacket {
    public static final NetworkBuffer.Type<AxiomClientAnnotationUpdatePacket> SERIALIZER = NetworkBufferTemplate.template(
            AnnotationUpdate.NETWORK_TYPE.list(Short.MAX_VALUE), AxiomClientAnnotationUpdatePacket::updates,
            AxiomClientAnnotationUpdatePacket::new);

    public AxiomClientAnnotationUpdatePacket {
        updates = List.copyOf(updates);
    }
}
