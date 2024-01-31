package net.hollowcube.terraform.compat.axiom.packet.client;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientMarkerNbtRequestPacket(
        @NotNull UUID uuid
) implements AxiomClientPacket {

    public AxiomClientMarkerNbtRequestPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(buffer.read(NetworkBuffer.UUID));
    }

}
