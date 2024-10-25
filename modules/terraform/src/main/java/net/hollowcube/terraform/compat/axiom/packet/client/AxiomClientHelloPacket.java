package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public record AxiomClientHelloPacket(
        int apiVersion,
        int dataVersion,
        int protocolVersion
) implements AxiomClientPacket {
    public static final NetworkBuffer.Type<AxiomClientHelloPacket> SERIALIZER = new NetworkBuffer.Type<>() {
        @Override
        public void write(@NotNull NetworkBuffer buffer, AxiomClientHelloPacket value) {
            buffer.write(NetworkBuffer.VAR_INT, value.apiVersion());
            buffer.write(NetworkBuffer.VAR_INT, value.dataVersion());
            buffer.write(NetworkBuffer.VAR_INT, value.protocolVersion());
        }

        @Override
        public AxiomClientHelloPacket read(@NotNull NetworkBuffer buffer) {
            int apiVersion = buffer.read(NetworkBuffer.VAR_INT);
            int dataVersion = buffer.read(NetworkBuffer.VAR_INT);
            if (apiVersion < 8) {
                buffer.read(NetworkBuffer.NBT); // Used to be NBT here just read and ignore.
                return new AxiomClientHelloPacket(apiVersion, dataVersion, 0);
            }
            return new AxiomClientHelloPacket(apiVersion, dataVersion, buffer.read(NetworkBuffer.VAR_INT));
        }
    };
}
