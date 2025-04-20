package net.hollowcube.compat.lunar.packets;

import com.google.gson.Gson;
import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public record ClientboundLunarPacket(
        @NotNull Map<String, ?> data
) implements ClientboundModPacket<ClientboundLunarPacket> {
    public static final String TYPE_PREFIX = "type.googleapis.com/lunarclient.apollo.";

    public static final Type<ClientboundLunarPacket> TYPE = Type.of(
            "apollo",
            "json",
            (buffer, packet) -> {
                final var json = new Gson().toJson(packet.data);
                buffer.write(NetworkBuffer.RAW_BYTES, json.getBytes(StandardCharsets.UTF_8));
            }
    );

    @Override
    public Type<ClientboundLunarPacket> getType() {
        return TYPE;
    }
}
