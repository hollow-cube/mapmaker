package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record AxiomServerboundTeleportPacket(
    String dimension,
    Pos position
) implements ServerboundModPacket<AxiomServerboundTeleportPacket> {

    public static final Type<AxiomServerboundTeleportPacket> TYPE = Type.of(
        AxiomAPI.CHANNEL, "teleport",
        NetworkBufferTemplate.template(
            NetworkBuffer.STRING, AxiomServerboundTeleportPacket::dimension,
            NetworkBuffer.POS, AxiomServerboundTeleportPacket::position,
            AxiomServerboundTeleportPacket::new
        )
    );

    @Override
    public Type<AxiomServerboundTeleportPacket> getType() {
        return TYPE;
    }
}
