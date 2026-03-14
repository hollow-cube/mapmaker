package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record AxiomServerboundSetFlySpeedPacket(
    float speed
) implements ServerboundModPacket<AxiomServerboundSetFlySpeedPacket> {

    public static final Type<AxiomServerboundSetFlySpeedPacket> TYPE = Type.of(
        AxiomAPI.CHANNEL, "set_fly_speed",
        NetworkBufferTemplate.template(
            NetworkBuffer.FLOAT, AxiomServerboundSetFlySpeedPacket::speed,
            AxiomServerboundSetFlySpeedPacket::new
        )
    );

    @Override
    public Type<AxiomServerboundSetFlySpeedPacket> getType() {
        return TYPE;
    }
}
