package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record AxiomServerboundSetTimePacket(
        @NotNull String dimension,
        @Nullable Integer time,
        @Nullable Boolean freezeTime
) implements ServerboundModPacket<AxiomServerboundSetTimePacket> {

    public static final Type<AxiomServerboundSetTimePacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "set_world_time",
            NetworkBufferTemplate.template(
                    NetworkBuffer.STRING, AxiomServerboundSetTimePacket::dimension,
                    NetworkBuffer.INT.optional(), AxiomServerboundSetTimePacket::time,
                    NetworkBuffer.BOOLEAN.optional(), AxiomServerboundSetTimePacket::freezeTime,
                    AxiomServerboundSetTimePacket::new
            )
    );

    @Override
    public Type<AxiomServerboundSetTimePacket> getType() {
        return TYPE;
    }
}
