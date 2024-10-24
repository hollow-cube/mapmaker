package net.hollowcube.terraform.compat.axiom.packet.client;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minestom.server.network.NetworkBuffer.*;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientSetTimePacket(
        @NotNull String dimensionName,
        @Nullable Integer time,
        @Nullable Boolean freezeTime
) implements AxiomClientPacket {

    // TODO: 1.21.2
//    public AxiomClientSetTimePacket(@NotNull NetworkBuffer buffer, int apiVersion) {
//        this(buffer.read(STRING), buffer.readOptional(INT), buffer.readOptional(BOOLEAN));
//    }

}
