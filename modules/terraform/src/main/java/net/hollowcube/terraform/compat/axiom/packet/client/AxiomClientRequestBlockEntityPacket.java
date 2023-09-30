package net.hollowcube.terraform.compat.axiom.packet.client;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.minestom.server.network.NetworkBuffer.LONG;
import static net.minestom.server.network.NetworkBuffer.STRING;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientRequestBlockEntityPacket(
        long sequence,
        @NotNull String dimensionName,
        @NotNull List<Long> positions
) implements AxiomClientPacket {

    public AxiomClientRequestBlockEntityPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(buffer.read(LONG), buffer.read(STRING), buffer.readCollection(LONG));
    }

}
