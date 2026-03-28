package net.hollowcube.compat.lunar.packets;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.lunarclient.apollo.player.v1.PlayerHandshakeMessage;
import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.ThrowingFunction;

import java.util.List;

// For lunar to advertise itself we have to ensure we register the channels
// Rather than send another register message, we'll just register some dummy handlers
// Who knows, maybe they'll be useful in the future if lunar add more serverbound packets
public record ServerboundLunarPacket(
    Message message
) implements ServerboundModPacket<ServerboundLunarPacket> {

    private static final List<Class<? extends Message>> SUPPORTED_MESSAGES = List.of(
        PlayerHandshakeMessage.class
    );
    private static final ThrowingFunction<NetworkBuffer, ServerboundLunarPacket> READER = buffer -> {
        var bytes = buffer.read(NetworkBuffer.RAW_BYTES);
        var any = Any.parseFrom(bytes);
        for (var type : SUPPORTED_MESSAGES) {
            if (any.is(type)) {
                return new ServerboundLunarPacket(any.unpack(type));
            }
        }
        return new ServerboundLunarPacket(any);
    };

    public static final Type<ServerboundLunarPacket> APOLLO_JSON_TYPE = Type.of("apollo", "json", READER);

    @Override
    public Type<ServerboundLunarPacket> getType() {
        return null;
    }
}
