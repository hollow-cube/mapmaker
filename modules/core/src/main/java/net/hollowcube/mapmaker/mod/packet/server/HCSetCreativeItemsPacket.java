package net.hollowcube.mapmaker.mod.packet.server;

import net.hollowcube.mapmaker.util.ProtocolUtil;
import net.minestom.server.item.Material;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.minestom.server.network.NetworkBuffer.STRING;
import static net.minestom.server.network.NetworkBuffer.VAR_INT;

@SuppressWarnings("UnstableApiUsage")
public record HCSetCreativeItemsPacket(
        @NotNull Map<String, List<Entry>> entriesByGroup
) implements HCServerConfigPacket {
    public static final String CHANNEL = "hollowcube:set_creative_items";

    @Override
    public @NotNull String packetChannel() {
        return CHANNEL;
    }

    @Override
    public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
        ProtocolUtil.writeMap(buffer, STRING, (entries, buf) -> buf.writeCollection(entries, (b, entry) -> entry.write(b)), entriesByGroup);
    }

    public enum Position {
        START, END,
        BEFORE, AFTER;

        public boolean hasRelative() {
            return this == BEFORE || this == AFTER;
        }
    }

    public record Entry(
            @NotNull Material material,
            int customModelData,
            @NotNull Position position,
            @Nullable Material relativeTo
    ) implements NetworkBuffer.Writer {

        @Override
        public void write(@NotNull NetworkBuffer writer) {
            writer.write(VAR_INT, material.id());
            writer.write(VAR_INT, customModelData);
            writer.writeEnum(Position.class, position);
            if (position.hasRelative()) {
                writer.write(VAR_INT, Objects.requireNonNull(relativeTo).id());
            }
        }
    }

}
