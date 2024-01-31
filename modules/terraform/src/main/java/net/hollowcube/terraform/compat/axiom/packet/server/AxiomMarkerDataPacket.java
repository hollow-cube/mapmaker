package net.hollowcube.terraform.compat.axiom.packet.server;

import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public record AxiomMarkerDataPacket(
        @NotNull List<Entry> entries,
        @NotNull List<UUID> removedMarkers
) implements AxiomServerPacket {

    @Override
    public @NotNull String packetChannel() {
        return "axiom:marker_data";
    }

    @Override
    public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
        buffer.writeCollection(entries, (b1, entry) -> entry.write(b1, apiVersion));
        buffer.writeCollection(removedMarkers, (b1, uuid) -> b1.write(NetworkBuffer.UUID, uuid));
    }

    public record Entry(
            @NotNull UUID uuid,
            @NotNull Point position,
            @Nullable String name,
            @Nullable Point regionMin,
            @Nullable Point regionMax
    ) {

        public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
            buffer.write(NetworkBuffer.UUID, uuid);
            buffer.write(NetworkBuffer.VECTOR3D, position);
            buffer.writeOptional(NetworkBuffer.STRING, name);
            if (regionMin != null && regionMax != null) {
                buffer.write(NetworkBuffer.BOOLEAN, true);
                buffer.write(NetworkBuffer.VECTOR3D, regionMin);
                buffer.write(NetworkBuffer.VECTOR3D, regionMax);
            } else {
                buffer.write(NetworkBuffer.BOOLEAN, false);
            }
        }

    }

}
