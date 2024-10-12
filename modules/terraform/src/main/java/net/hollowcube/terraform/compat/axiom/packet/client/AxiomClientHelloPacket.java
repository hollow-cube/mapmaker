package net.hollowcube.terraform.compat.axiom.packet.client;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public record AxiomClientHelloPacket(
        int apiVersion,
        int dataVersion,
        int protocolVersion
) implements AxiomClientPacket {

    public AxiomClientHelloPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(read(buffer, apiVersion));
    }

    public AxiomClientHelloPacket(@NotNull AxiomClientHelloPacket packet) {
        this(packet.apiVersion(), packet.dataVersion(), packet.protocolVersion());
    }

    private static @NotNull AxiomClientHelloPacket read(@NotNull NetworkBuffer buffer, int alwaysZeroDoNotUse) {
        int apiVersion = buffer.read(NetworkBuffer.VAR_INT);
        int dataVersion = buffer.read(NetworkBuffer.VAR_INT);
        if (apiVersion < 8) {
            buffer.read(NetworkBuffer.NBT); // Used to be NBT here just read and ignore.
            return new AxiomClientHelloPacket(apiVersion, dataVersion, 0);
        }
        return new AxiomClientHelloPacket(apiVersion, dataVersion, buffer.read(NetworkBuffer.VAR_INT));
    }

}
