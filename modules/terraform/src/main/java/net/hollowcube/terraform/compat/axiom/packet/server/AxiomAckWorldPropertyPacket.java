package net.hollowcube.terraform.compat.axiom.packet.server;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;

@SuppressWarnings("UnstableApiUsage")
public record AxiomAckWorldPropertyPacket(
        int sequenceId
) implements AxiomServerPacket {
    @Override
    public @NotNull String packetChannel() {
        return "axiom:ack_world_properties";
    }

    @Override
    public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
        buffer.write(VAR_INT, sequenceId);
    }
}
