package net.hollowcube.terraform.compat.axiom.packet.server;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public record AxiomMarkerNbtResponsePacket(
        @NotNull UUID uuid,
        @NotNull NBTCompound data
) implements AxiomServerPacket {

    @Override
    public @NotNull String packetChannel() {
        return "axiom:marker_nbt_response";
    }

    @Override
    public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
        buffer.write(NetworkBuffer.UUID, uuid);
        buffer.write(NetworkBuffer.NBT, data);
    }

}
