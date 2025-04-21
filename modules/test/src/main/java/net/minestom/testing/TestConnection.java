package net.minestom.testing;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.PlayerProvider;
import net.minestom.server.network.packet.server.ServerPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TestConnection {
    @NotNull Player connect(@Nullable PlayerProvider provider, @NotNull Instance instance, @NotNull Pos pos);

    <T extends ServerPacket> @NotNull Collector<T> trackIncoming(@NotNull Class<T> type);

    default @NotNull Collector<ServerPacket> trackIncoming() {
        return trackIncoming(ServerPacket.class);
    }
}
