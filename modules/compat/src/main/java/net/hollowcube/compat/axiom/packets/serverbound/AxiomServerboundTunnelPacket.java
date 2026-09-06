package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record AxiomServerboundTunnelPacket(
        byte @NotNull [] fragment
) implements ServerboundModPacket<AxiomServerboundTunnelPacket> {

    public static final Type<AxiomServerboundTunnelPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "tunnel",
            NetworkBufferTemplate.template(
                    NetworkBuffer.RAW_BYTES, AxiomServerboundTunnelPacket::fragment,
                    AxiomServerboundTunnelPacket::new
            )
    );

    @Override
    public Type<AxiomServerboundTunnelPacket> getType() {
        return TYPE;
    }
}
