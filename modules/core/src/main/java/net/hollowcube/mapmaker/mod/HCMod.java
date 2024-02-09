package net.hollowcube.mapmaker.mod;

import net.hollowcube.mapmaker.mod.packet.client.HCClientModifyAnimationPacket;
import net.hollowcube.mapmaker.mod.packet.client.HCClientPacket;
import net.hollowcube.mapmaker.mod.packet.client.HCClientPlayPacket;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class HCMod {
    private static final Map<String, ReadableHCPacket<? extends HCClientPlayPacket>> CLIENT_PLAY_PACKETS = Map.ofEntries(
            Map.entry("hollowcube:modify_animation", HCClientModifyAnimationPacket::new)
    );

    private interface ReadableHCPacket<T extends HCClientPacket> {
        @NotNull T read(@NotNull NetworkBuffer buffer, int apiVersion);
    }

    public static @Nullable HCClientPlayPacket readPlayPacket(@NotNull PlayerPluginMessageEvent event) {
        var reader = CLIENT_PLAY_PACKETS.get(event.getIdentifier());
        if (reader == null) return null;
        var buffer = new NetworkBuffer(ByteBuffer.wrap(event.getMessage()));

        // When we receive the hello packet there will be no client info, so default to
        // the highest supported API version. It is present in the hello packet anyway.
//        var clientInfo = event.getPlayer().getTag(CLIENT_INFO_TAG);
//        var apiVersion = clientInfo == null ? MAX_API_VERSION : clientInfo.apiVersion();
        return reader.read(buffer, 0);
    }

}
