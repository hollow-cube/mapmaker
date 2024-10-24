package net.hollowcube.terraform.compat.axiom.packet.client;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientDeleteEntitiesPacket(
        @NotNull List<@NotNull UUID> uuids
) implements AxiomClientPacket {
    private static final int MAX_ENTRIES = 1024;

    public AxiomClientDeleteEntitiesPacket {
        uuids = List.copyOf(uuids);
    }

    // TODO: 1.21.2
//    public AxiomClientDeleteEntitiesPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
//        this(buffer.readCollection(NetworkBuffer.UUID, MAX_ENTRIES));
//    }
}
