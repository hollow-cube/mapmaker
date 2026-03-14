package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record AxiomServerboundSetNoPhysicalTriggerPacket(
    boolean noPhysicalTrigger
) implements ServerboundModPacket<AxiomServerboundSetNoPhysicalTriggerPacket> {

    public static final Type<AxiomServerboundSetNoPhysicalTriggerPacket> TYPE = Type.of(
        AxiomAPI.CHANNEL, "set_no_physical_trigger",
        NetworkBufferTemplate.template(
            NetworkBuffer.BOOLEAN, AxiomServerboundSetNoPhysicalTriggerPacket::noPhysicalTrigger,
            AxiomServerboundSetNoPhysicalTriggerPacket::new
        )
    );

    @Override
    public Type<AxiomServerboundSetNoPhysicalTriggerPacket> getType() {
        return TYPE;
    }
}
