package net.hollowcube.terraform.compat.axiom.packet.client;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.BYTE;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientSetGameModePacket(
        byte gameModeId
) implements AxiomClientPacket {

    public AxiomClientSetGameModePacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(buffer.read(BYTE));
    }

}
