package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.hollowcube.compat.axiom.AxiomPlayer;
import net.minestom.server.entity.Player;

public interface AxiomClientboundModPacket<T extends AxiomClientboundModPacket<T>> extends ClientboundModPacket<T> {

    @Override
    default void send(Player player, boolean force) {
        var axiom = AxiomPlayer.get(player);
        if (!axiom.isEnabled()) return;
        ClientboundModPacket.super.send(player, force || axiom.api() == AxiomAPI.API_10);
    }
}
