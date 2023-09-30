package net.hollowcube.terraform.compat.axiom.packet.client;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.FLOAT;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientSetFlySpeedPacket(
        float flySpeed
) implements AxiomClientPacket {

    public AxiomClientSetFlySpeedPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(buffer.read(FLOAT));
    }

}
