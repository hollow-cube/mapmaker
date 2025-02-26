package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.entity.GameMode;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.EnumSet;

public record AxiomClientboundAllowedGamemodesPacket(
    EnumSet<GameMode> allowed
) implements AxiomClientboundModPacket<AxiomClientboundAllowedGamemodesPacket> {

    public static final Type<AxiomClientboundAllowedGamemodesPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "allowed_gamemodes",
            NetworkBufferTemplate.template(
                    NetworkBuffer.EnumSet(GameMode.class), AxiomClientboundAllowedGamemodesPacket::allowed,
                    AxiomClientboundAllowedGamemodesPacket::new
            )
    );

    @Override
    public Type<AxiomClientboundAllowedGamemodesPacket> getType() {
        return TYPE;
    }
}
