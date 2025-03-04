package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.color.AlphaColor;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record AxiomClientboundMarkerDataPacket(
    @NotNull List<Entry> entries,
    @NotNull List<UUID> removed
) implements AxiomClientboundModPacket<AxiomClientboundMarkerDataPacket> {

    public static final Type<AxiomClientboundMarkerDataPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "marker_data",
            NetworkBufferTemplate.template(
                    Entry.CODEC.list(), AxiomClientboundMarkerDataPacket::entries,
                    NetworkBuffer.UUID.list(), AxiomClientboundMarkerDataPacket::removed,
                    AxiomClientboundMarkerDataPacket::new
            )
    );

    public static AxiomClientboundMarkerDataPacket spawnMarker(UUID id, Point pos) {
        return new AxiomClientboundMarkerDataPacket(List.of(new Entry(id, pos, null, null, null, null, null, 0)), List.of());
    }

    public static AxiomClientboundMarkerDataPacket updateMarker(
            UUID id, Point pos, String name, Point min, Point max,
            AlphaColor lineColor, AlphaColor fillColor, float lineWidth
    ) {
        return new AxiomClientboundMarkerDataPacket(List.of(new Entry(id, pos, name, min, max, lineColor, fillColor, lineWidth)), List.of());
    }

    public static AxiomClientboundMarkerDataPacket removeMarker(UUID id) {
        return new AxiomClientboundMarkerDataPacket(List.of(), List.of(id));
    }

    @Override
    public Type<AxiomClientboundMarkerDataPacket> getType() {
        return TYPE;
    }

    public record Entry(
            @NotNull UUID id,
            @NotNull Point pos,
            @Nullable String name,

            @Nullable Point min,
            @Nullable Point max,

            @Nullable AlphaColor lineColor,
            @Nullable AlphaColor fillColor,
            float lineWidth
    ) {

        public static final NetworkBuffer.Type<Entry> CODEC = new NetworkBuffer.Type<>() {
            @Override
            public void write(@NotNull NetworkBuffer buffer, Entry entry) {
                buffer.write(NetworkBuffer.UUID, entry.id);
                buffer.write(NetworkBuffer.VECTOR3D, entry.pos);
                buffer.write(NetworkBuffer.STRING.optional(), entry.name);

                if (entry.min != null && entry.max != null) {
                    byte flags = 1;
                    if (entry.lineColor != null) flags |= 2;
                    if (entry.fillColor != null) flags |= 8;
                    if (entry.lineWidth != 0) flags |= 4;

                    buffer.write(NetworkBuffer.BYTE, flags);
                    buffer.write(NetworkBuffer.VECTOR3D, entry.min);
                    buffer.write(NetworkBuffer.VECTOR3D, entry.max);

                    if (entry.lineColor != null) buffer.write(Color.NETWORK_TYPE, entry.lineColor);
                    if (entry.lineWidth != 0) buffer.write(NetworkBuffer.FLOAT, entry.lineWidth);
                    if (entry.fillColor != null) buffer.write(Color.NETWORK_TYPE, entry.fillColor);
                } else {
                    buffer.write(NetworkBuffer.BYTE, (byte) 0);
                }
            }

            @Override
            public Entry read(@NotNull NetworkBuffer buffer) {
                UUID id = buffer.read(NetworkBuffer.UUID);
                Point pos = buffer.read(NetworkBuffer.VECTOR3D);
                String name = buffer.read(NetworkBuffer.STRING.optional());
                byte flags = buffer.read(NetworkBuffer.BYTE);

                Point min = flags != 0 ? buffer.read(NetworkBuffer.VECTOR3D) : null;
                Point max = flags != 0 ? buffer.read(NetworkBuffer.VECTOR3D) : null;

                AlphaColor lineColor = (flags & 2) != 0 ? buffer.read(AlphaColor.NETWORK_TYPE) : null;
                float lineWidth = (flags & 4) != 0 ? buffer.read(NetworkBuffer.FLOAT) : 0;
                AlphaColor fillColor = (flags & 8) != 0 ? buffer.read(AlphaColor.NETWORK_TYPE) : null;

                return new Entry(id, pos, name, min, max, lineColor, fillColor, lineWidth);
            }
        };

    }
}
