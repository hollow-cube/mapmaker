package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record AxiomClientSetEditorViewsPacket(
        @NotNull UUID uuid,
        @NotNull List<View> views
) implements AxiomClientPacket {
    public static final NetworkBuffer.Type<AxiomClientSetEditorViewsPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, AxiomClientSetEditorViewsPacket::uuid,
            View.SERIALIZER.list(1024), AxiomClientSetEditorViewsPacket::views,
            AxiomClientSetEditorViewsPacket::new);

    public record View(
            @NotNull String name,
            @NotNull UUID uuid,
            boolean pinLevel,
            @Nullable String dimensionName,
            boolean pinLocation,
            @Nullable Pos location
    ) {
        public static final NetworkBuffer.Type<View> SERIALIZER = NetworkBufferTemplate.template(
                NetworkBuffer.STRING, View::name,
                NetworkBuffer.UUID, View::uuid,
                NetworkBuffer.BOOLEAN, View::pinLevel,
                NetworkBuffer.STRING.optional(), View::dimensionName,
                NetworkBuffer.BOOLEAN, View::pinLocation,
                NetworkBuffer.POS.optional(), View::location,
                View::new);
    }
}
