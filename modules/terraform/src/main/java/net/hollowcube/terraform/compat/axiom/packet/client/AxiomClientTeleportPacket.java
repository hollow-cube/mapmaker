package net.hollowcube.terraform.compat.axiom.packet.client;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.*;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientTeleportPacket(
        @NotNull String dimensionName,
        @NotNull Pos position
) implements AxiomClientPacket {

    public AxiomClientTeleportPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(buffer.read(STRING), new Pos(
                buffer.read(DOUBLE),
                buffer.read(DOUBLE),
                buffer.read(DOUBLE),
                buffer.read(FLOAT),
                buffer.read(FLOAT)
        ));
    }
}
