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
    private static final int FLAG_REGION = 1;
    private static final int FLAG_LINE_COLOR = 2;
    private static final int FLAG_LINE_THICKNESS = 4;
    private static final int FLAG_FACE_COLOR = 8;

    public AxiomMarkerDataPacket(@NotNull Entry entry) {
        this(List.of(entry), List.of());
    }

    public AxiomMarkerDataPacket(@NotNull UUID removedMarker) {
        this(List.of(), List.of(removedMarker));
    }

    @Override
    public @NotNull String packetChannel() {
        return "axiom:marker_data";
    }

    @Override
    public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
        // TODO: 1.21.2
//        buffer.writeCollection(entries, (b1, entry) -> entry.write(b1, apiVersion));
//        buffer.writeCollection(removedMarkers, (b1, uuid) -> b1.write(NetworkBuffer.UUID, uuid));
    }

    public record Entry(
            @NotNull UUID uuid,
            @NotNull Point position, @Nullable String name,
            @Nullable Point regionMin, @Nullable Point regionMax,
            int lineColor, float lineThickness, int faceColor
    ) {

        public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
            buffer.write(NetworkBuffer.UUID, uuid);
            buffer.write(NetworkBuffer.VECTOR3D, position);
            // TODO: 1.21.2
//            buffer.writeOptional(NetworkBuffer.STRING, name);
            if (regionMin != null && regionMax != null) {
                // Note: All flags besides FLAG_REGION are only valid to write if the region is present
                //       that's why this logic looks slightly odd.
                buffer.write(NetworkBuffer.BYTE, (byte) (FLAG_REGION
                        | (lineColor != 0 ? FLAG_LINE_COLOR : 0)
                        | (lineThickness != 0 ? FLAG_LINE_THICKNESS : 0)
                        | (faceColor != 0 ? FLAG_FACE_COLOR : 0)
                ));
                buffer.write(NetworkBuffer.VECTOR3D, regionMin);
                buffer.write(NetworkBuffer.VECTOR3D, regionMax);
                if (lineColor != 0) buffer.write(NetworkBuffer.INT, lineColor);
                if (lineThickness != 0) buffer.write(NetworkBuffer.FLOAT, lineThickness);
                if (faceColor != 0) buffer.write(NetworkBuffer.INT, faceColor);
            } else {
                buffer.write(NetworkBuffer.BYTE, (byte) 0);
            }
        }
    }

}
