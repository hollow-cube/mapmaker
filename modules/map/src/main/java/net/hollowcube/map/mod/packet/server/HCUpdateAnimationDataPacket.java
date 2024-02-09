package net.hollowcube.map.mod.packet.server;

import net.hollowcube.map.animation.property.KeyframeSequence;
import net.hollowcube.mapmaker.mod.packet.server.HCServerPlayPacket;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public record HCUpdateAnimationDataPacket(
        @NotNull List<Entry> entries
) implements HCServerPlayPacket {

    public HCUpdateAnimationDataPacket(@NotNull Entry... entries) {
        this(List.of(entries));
    }

    @Override
    public @NotNull String packetChannel() {
        return "hollowcube:update_animation_data";
    }

    @Override
    public void write0(@NotNull NetworkBuffer buffer) {
        buffer.writeCollection(entries, (b1, e) -> e.write(b1));
    }

    public sealed interface Entry {
        void write(@NotNull NetworkBuffer buffer);
    }

    public record AddObject(
            @NotNull UUID id, int entityId,
            @NotNull List<KeyframeSequence<?>> properties
    ) implements Entry {
        private static final int ID = 0;

        @Override
        public void write(@NotNull NetworkBuffer buffer) {
            buffer.write(NetworkBuffer.VAR_INT, ID);
            buffer.write(NetworkBuffer.UUID, id);
            buffer.write(NetworkBuffer.VAR_INT, entityId);
            buffer.writeCollection(properties, (b1, seq) -> seq.write(b1));
        }
    }

    public record UpdateProperty(
            @NotNull UUID objectId,
            @NotNull KeyframeSequence<?> property
    ) implements Entry {
        private static final int ID = 1;

        @Override
        public void write(@NotNull NetworkBuffer buffer) {
            buffer.write(NetworkBuffer.VAR_INT, ID);
            buffer.write(NetworkBuffer.UUID, objectId);
            property.write(buffer);
        }
    }
}
