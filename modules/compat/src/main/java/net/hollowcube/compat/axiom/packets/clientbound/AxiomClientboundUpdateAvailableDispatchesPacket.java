package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record AxiomClientboundUpdateAvailableDispatchesPacket(
    int add,
    int max
) implements AxiomClientboundModPacket<AxiomClientboundUpdateAvailableDispatchesPacket> {

    public static final Type<AxiomClientboundUpdateAvailableDispatchesPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "update_available_dispatch_sends",
            NetworkBufferTemplate.template(
                    NetworkBuffer.VAR_INT, AxiomClientboundUpdateAvailableDispatchesPacket::add,
                    NetworkBuffer.VAR_INT, AxiomClientboundUpdateAvailableDispatchesPacket::max,
                    AxiomClientboundUpdateAvailableDispatchesPacket::new
            )
    );

    @Override
    public Type<AxiomClientboundUpdateAvailableDispatchesPacket> getType() {
        return TYPE;
    }
}
