package net.hollowcube.terraform.compat.axiom.packet.client;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.*;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientSetWorldPropertyPacket(
        @NotNull NamespaceID id,
        int typeId,
        byte[] value,
        int sequenceId
) implements AxiomClientPacket {

    public AxiomClientSetWorldPropertyPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(NamespaceID.from(buffer.read(STRING)), buffer.read(VAR_INT), buffer.read(BYTE_ARRAY), buffer.read(VAR_INT));
    }
}
