package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.axiom.AxiomPlayer;
import net.minestom.server.entity.Player;

public interface AxiomClientboundModPacket<T extends AxiomClientboundModPacket<T>> extends ClientboundModPacket<T> {

    @Override
    default void send(Player player, boolean force) {
        if (!AxiomPlayer.isEnabled(player)) return;
        ClientboundModPacket.super.send(player, force);
    }
}
