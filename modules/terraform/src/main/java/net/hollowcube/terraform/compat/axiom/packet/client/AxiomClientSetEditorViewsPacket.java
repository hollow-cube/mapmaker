package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.util.ProtocolUtil;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientSetEditorViewsPacket(
        @NotNull UUID uuid,
        @NotNull List<View> views
) implements AxiomClientPacket {
    private static final int MAX_VIEWS = 1024;

    public AxiomClientSetEditorViewsPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(buffer.read(NetworkBuffer.UUID), buffer.readCollection(b1 -> new View(b1, apiVersion), MAX_VIEWS));
    }

    public record View(
            @NotNull String name,
            @NotNull UUID uuid,
            boolean pinLevel,
            @Nullable String dimensionName,
            boolean pinLocation,
            @Nullable Pos location
    ) {

        public View(@NotNull NetworkBuffer buffer, int apiVersion) {
            this(buffer.read(NetworkBuffer.STRING), buffer.read(NetworkBuffer.UUID),
                    buffer.read(NetworkBuffer.BOOLEAN), buffer.readOptional(NetworkBuffer.STRING),
                    buffer.read(NetworkBuffer.BOOLEAN), buffer.readOptional(ProtocolUtil::readPos));
        }
    }
}
