package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.axiom.AxiomAPI;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record AxiomClientboundMarkerResponsePacket(
    @NotNull UUID uuid,
    @NotNull CompoundBinaryTag data
) implements AxiomClientboundModPacket<AxiomClientboundMarkerResponsePacket> {

    public static final Type<AxiomClientboundMarkerResponsePacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "marker_nbt_response",
            NetworkBufferTemplate.template(
                    NetworkBuffer.UUID, AxiomClientboundMarkerResponsePacket::uuid,
                    NetworkBuffer.NBT_COMPOUND, AxiomClientboundMarkerResponsePacket::data,
                    AxiomClientboundMarkerResponsePacket::new
            )
    );

    @Override
    public Type<AxiomClientboundMarkerResponsePacket> getType() {
        return TYPE;
    }
}
