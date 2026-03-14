package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.entity.GameMode;
import net.minestom.server.network.NetworkBufferTemplate;

public record AxiomServerboundSetGameModePacket(
    GameMode gameMode
) implements ServerboundModPacket<AxiomServerboundSetGameModePacket> {

    public static final Type<AxiomServerboundSetGameModePacket> TYPE = Type.of(
        AxiomAPI.CHANNEL, "set_gamemode",
        NetworkBufferTemplate.template(
            GameMode.NETWORK_TYPE, AxiomServerboundSetGameModePacket::gameMode,
            AxiomServerboundSetGameModePacket::new
        )
    );

    @Override
    public Type<AxiomServerboundSetGameModePacket> getType() {
        return TYPE;
    }
}
