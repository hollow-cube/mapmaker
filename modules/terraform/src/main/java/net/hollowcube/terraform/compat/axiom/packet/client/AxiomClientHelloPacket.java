package net.hollowcube.terraform.compat.axiom.packet.client;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import static net.minestom.server.network.NetworkBuffer.NBT;
import static net.minestom.server.network.NetworkBuffer.VAR_INT;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientHelloPacket(
        int apiVersion,
        int dataVersion,
        @NotNull NBTCompound extraData
) implements AxiomClientPacket {

    public AxiomClientHelloPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        // API version here is always the max api version, so should not be used.
        this(buffer.read(VAR_INT), buffer.read(VAR_INT), (NBTCompound) buffer.read(NBT));
    }

}
