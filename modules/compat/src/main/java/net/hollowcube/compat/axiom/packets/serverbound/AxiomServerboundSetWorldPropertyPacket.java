package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

public record AxiomServerboundSetWorldPropertyPacket(
        @NotNull NamespaceID id,
        int type,
        byte[] value,
        int sequence
) implements ServerboundModPacket<AxiomServerboundSetWorldPropertyPacket> {

    public static final Type<AxiomServerboundSetWorldPropertyPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "set_world_property",
            NetworkBufferTemplate.template(
                    NetworkBuffer.STRING.transform(NamespaceID::from, NamespaceID::asString), AxiomServerboundSetWorldPropertyPacket::id,
                    NetworkBuffer.VAR_INT, AxiomServerboundSetWorldPropertyPacket::type,
                    NetworkBuffer.BYTE_ARRAY, AxiomServerboundSetWorldPropertyPacket::value,
                    NetworkBuffer.VAR_INT, AxiomServerboundSetWorldPropertyPacket::sequence,
                    AxiomServerboundSetWorldPropertyPacket::new
            )
    );

    @Override
    public Type<AxiomServerboundSetWorldPropertyPacket> getType() {
        return TYPE;
    }
}
