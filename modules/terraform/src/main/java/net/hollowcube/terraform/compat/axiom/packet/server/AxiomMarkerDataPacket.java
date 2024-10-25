package net.hollowcube.terraform.compat.axiom.packet.server;

import net.hollowcube.terraform.compat.axiom.packet.AxiomServerPacket;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record AxiomMarkerDataPacket(
        @NotNull List<Entry> entries,
        @NotNull List<UUID> removedMarkers
) implements AxiomServerPacket {
    public static final NetworkBuffer.Type<AxiomMarkerDataPacket> SERIALIZER = NetworkBufferTemplate.template(
            Entry.SERIALIZER.list(Short.MAX_VALUE), AxiomMarkerDataPacket::entries,
            NetworkBuffer.UUID.list(Short.MAX_VALUE), AxiomMarkerDataPacket::removedMarkers,
            AxiomMarkerDataPacket::new);

    public AxiomMarkerDataPacket(@NotNull Entry entry) {
        this(List.of(entry), List.of());
    }

    public AxiomMarkerDataPacket(@NotNull UUID removedMarker) {
        this(List.of(), List.of(removedMarker));
    }

    public record Entry(
            @NotNull UUID uuid,
            @NotNull Point position, @Nullable String name,
            @Nullable Point regionMin, @Nullable Point regionMax,
            int lineColor, float lineThickness, int faceColor
    ) {
        private static final int FLAG_REGION = 1;
        private static final int FLAG_LINE_COLOR = 2;
        private static final int FLAG_LINE_THICKNESS = 4;
        private static final int FLAG_FACE_COLOR = 8;

        public static final NetworkBuffer.Type<Entry> SERIALIZER = new NetworkBuffer.Type<>() {
            private static final NetworkBuffer.Type<String> OPTIONAL_STRING = NetworkBuffer.STRING.optional();

            @Override
            public void write(@NotNull NetworkBuffer buffer, Entry value) {
                buffer.write(NetworkBuffer.UUID, value.uuid);
                buffer.write(NetworkBuffer.VECTOR3D, value.position);
                buffer.write(OPTIONAL_STRING, value.name);
                if (value.regionMin != null && value.regionMax != null) {
                    // Note: All flags besides FLAG_REGION are only valid to write if the region is present
                    //       that's why this logic looks slightly odd.
                    buffer.write(NetworkBuffer.BYTE, (byte) (FLAG_REGION
                            | (value.lineColor != 0 ? FLAG_LINE_COLOR : 0)
                            | (value.lineThickness != 0 ? FLAG_LINE_THICKNESS : 0)
                            | (value.faceColor != 0 ? FLAG_FACE_COLOR : 0)
                    ));
                    buffer.write(NetworkBuffer.VECTOR3D, value.regionMin);
                    buffer.write(NetworkBuffer.VECTOR3D, value.regionMax);
                    if (value.lineColor != 0) buffer.write(NetworkBuffer.INT, value.lineColor);
                    if (value.lineThickness != 0) buffer.write(NetworkBuffer.FLOAT, value.lineThickness);
                    if (value.faceColor != 0) buffer.write(NetworkBuffer.INT, value.faceColor);
                } else {
                    buffer.write(NetworkBuffer.BYTE, (byte) 0); // flags
                }
            }

            @Override
            public Entry read(@NotNull NetworkBuffer buffer) {
                UUID uuid = buffer.read(NetworkBuffer.UUID);
                Point position = buffer.read(NetworkBuffer.VECTOR3D);
                String name = buffer.read(OPTIONAL_STRING);
                byte flags = buffer.read(NetworkBuffer.BYTE);
                Point regionMin = null;
                Point regionMax = null;
                int lineColor = 0;
                float lineThickness = 0;
                int faceColor = 0;
                if ((flags & FLAG_REGION) != 0) {
                    regionMin = buffer.read(NetworkBuffer.VECTOR3D);
                    regionMax = buffer.read(NetworkBuffer.VECTOR3D);
                    if ((flags & FLAG_LINE_COLOR) != 0) lineColor = buffer.read(NetworkBuffer.INT);
                    if ((flags & FLAG_LINE_THICKNESS) != 0) lineThickness = buffer.read(NetworkBuffer.FLOAT);
                    if ((flags & FLAG_FACE_COLOR) != 0) faceColor = buffer.read(NetworkBuffer.INT);
                }
                return new Entry(uuid, position, name, regionMin, regionMax, lineColor, lineThickness, faceColor);
            }
        };

        public Entry(@NotNull UUID uuid, @NotNull Point position, @Nullable String name, @Nullable Point regionMin, @Nullable Point regionMax) {
            this(uuid, position, name, regionMin, regionMax, 0, 0, 0);
        }

        public Entry(@NotNull UUID uuid, @NotNull Point position, @Nullable String name) {
            this(uuid, position, name, null, null, 0, 0, 0);
        }

        public Entry(@NotNull UUID uuid, @NotNull Point position) {
            this(uuid, position, null, null, null, 0, 0, 0);
        }
    }

}
