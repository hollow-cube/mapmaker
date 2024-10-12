package net.hollowcube.terraform.compat.axiom.packet.client;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public record AxiomClientAnnotationUpdatePacket(

) implements AxiomClientPacket {

    public AxiomClientAnnotationUpdatePacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this();
        // TODO
    }
}
