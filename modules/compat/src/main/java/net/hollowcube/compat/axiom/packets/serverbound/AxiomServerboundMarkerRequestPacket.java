package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.UUID;

public record AxiomServerboundMarkerRequestPacket(
    UUID id,
    Reason reason
) implements ServerboundModPacket<AxiomServerboundMarkerRequestPacket> {

    public static final Type<AxiomServerboundMarkerRequestPacket> TYPE = Type.of(
        AxiomAPI.CHANNEL, "marker_nbt_request",
        NetworkBufferTemplate.template(
            NetworkBuffer.UUID, AxiomServerboundMarkerRequestPacket::id,
            NetworkBuffer.VAR_INT.transform(Reason::fromId, Reason::id), AxiomServerboundMarkerRequestPacket::reason,
            AxiomServerboundMarkerRequestPacket::new
        )
    );

    @Override
    public Type<AxiomServerboundMarkerRequestPacket> getType() {
        return TYPE;
    }

    public enum Reason {
        UNKNOWN(-1),
        COPYING(0),
        RIGHT_CLICK(1),
        ;

        private final int id;

        Reason(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static Reason fromId(int id) {
            for (Reason reason : values()) {
                if (reason.id == id) {
                    return reason;
                }
            }
            return UNKNOWN;
        }
    }
}
