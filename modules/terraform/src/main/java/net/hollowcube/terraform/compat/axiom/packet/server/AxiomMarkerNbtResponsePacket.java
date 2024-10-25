package net.hollowcube.terraform.compat.axiom.packet.server;

import net.hollowcube.terraform.compat.axiom.packet.AxiomServerPacket;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record AxiomMarkerNbtResponsePacket(
        @NotNull UUID uuid,
        @NotNull CompoundBinaryTag data
) implements AxiomServerPacket {
    public static final NetworkBuffer.Type<AxiomMarkerNbtResponsePacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, AxiomMarkerNbtResponsePacket::uuid,
            NetworkBuffer.NBT_COMPOUND, AxiomMarkerNbtResponsePacket::data,
            AxiomMarkerNbtResponsePacket::new);
}
