package net.hollowcube.terraform.compat.axiom.packet.server;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public record AxiomAnnotationUpdatePacket(
) implements AxiomServerPacket {
    @Override
    public @NotNull String packetChannel() {
        return "axiom:annotation_update";
    }

    @Override
    public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
        // TODO
    }
}
